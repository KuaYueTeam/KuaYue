package cn.kycraft.kuayue.content.techtree.net;

import cn.kycraft.kuayue.content.techtree.TechTreeSystem;
import cn.kycraft.kuayue.content.techtree.CheckResult;
import cn.kycraft.kuayue.content.techtree.ClientTreeState;
import cn.kycraft.kuayue.content.techtree.ServerCheck;
import cn.kycraft.kuayue.content.techtree.ServerUnlock;
import cn.kycraft.kuayue.content.techtree.TreeSnapshot;
import lib.kasuga.core.networking.rpc.RpcApi;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.concurrent.CompletableFuture;

/**
 * All tech tree traffic, expressed as KasugaLibNext RPC calls.
 *
 * <p>Replaces the reference implementation's own {@code S2CPacket}/{@code C2SPacket} channel plus the
 * hand-written handshake/batch/EOF state machine: request/response correlation, timeouts and the
 * payload registration are owned by {@link RpcApi.WithResponse}.</p>
 */
public final class TechTreeRpc {

    private static boolean registered;

    private static final OpenRequest OPEN = new OpenRequest();
    private static final CanUnlockRequest CAN_UNLOCK = new CanUnlockRequest();
    private static final UnlockNodeRequest UNLOCK_NODE = new UnlockNodeRequest();
    private static final UnlockGroupRequest UNLOCK_GROUP = new UnlockGroupRequest();

    private TechTreeRpc() {
    }

    /** Wires the RPC endpoints into the tech tree registry group; idempotent. */
    public static synchronized void register(lib.kasuga.registration.Reg<?, ?> parent) {
        if (registered) return;
        registered = true;
        OPEN.register(parent);
        CAN_UNLOCK.register(parent);
        UNLOCK_NODE.register(parent);
        UNLOCK_GROUP.register(parent);
    }

    public static OpenRequest open() {
        return OPEN;
    }

    public static CanUnlockRequest canUnlock() {
        return CAN_UNLOCK;
    }

    public static UnlockNodeRequest unlockNode() {
        return UNLOCK_NODE;
    }

    public static UnlockGroupRequest unlockGroup() {
        return UNLOCK_GROUP;
    }

    /** Server -> client refresh, used when visibility changes without a client request. */
    public static void pushSnapshot(net.minecraft.server.level.ServerPlayer player,
                                   TreeSnapshot snapshot) {
        SNAPSHOT_PUSH.call(new SnapshotPayload(snapshot), player);
    }

    private static final SnapshotPush SNAPSHOT_PUSH = new SnapshotPush();

    public record SnapshotPayload(TreeSnapshot snapshot) {
        public static final StreamCodec<FriendlyByteBuf, SnapshotPayload> CODEC = StreamCodec.of(
                (buf, value) -> TreeSnapshot.CODEC.encode((RegistryFriendlyByteBuf) buf, value.snapshot()),
                buf -> new SnapshotPayload(TreeSnapshot.CODEC.decode((RegistryFriendlyByteBuf) buf)));
    }

    /** Fire-and-forget payload carrying a fresh snapshot. */
    public static final class SnapshotPush
            extends RpcApi.WithOutResponse<SnapshotPayload, SnapshotPush> {
        public SnapshotPush() {
            super("tech_tree/snapshot");
            executeOnClient();
        }

        @Override
        protected StreamCodec<FriendlyByteBuf, SnapshotPayload> getRequestPayloadCodec() {
            return SnapshotPayload.CODEC;
        }

        @Override
        public void handleInstant(SnapshotPayload request, IPayloadContext context) {
            ClientTreeState.apply(request.snapshot());
        }
    }

    public record OpenPayload(boolean creative) {
        public static final StreamCodec<FriendlyByteBuf, OpenPayload> CODEC = StreamCodec.of(
                (buf, value) -> buf.writeBoolean(value.creative()),
                buf -> new OpenPayload(buf.readBoolean()));
    }

    /** {@code client -> server}: "I opened the blueprint table, send me the tree". */
    public static final class OpenRequest extends RpcApi.WithResponse<OpenPayload, TreeSnapshot, OpenRequest> {
        public OpenRequest() {
            super("tech_tree/open");
            executeOnServer();
            runOnMainThread();
        }

        @Override
        protected StreamCodec<? super FriendlyByteBuf, OpenPayload> getRequestPayloadCodec() {
            return OpenPayload.CODEC;
        }

        @Override
        protected StreamCodec<? super FriendlyByteBuf, TreeSnapshot> getResponsePayloadCodec() {
            return TreeSnapshot.CODEC;
        }

        @Override
        public TreeSnapshot handleInstant(OpenPayload request, IPayloadContext context) {
            if (context.player() instanceof net.minecraft.server.level.ServerPlayer player) {
                return TreeSnapshot.build(player);
            }
            throw new IllegalStateException("Tech tree open requested without a server player");
        }

        public void request() {
            run(new OpenPayload(false)).whenComplete((snapshot, error) -> {
                if (error != null) {
                    TechTreeSystem.LOGGER.warn("Tech tree request failed", error);
                    return;
                }
                ClientTreeState.apply(snapshot);
            });
        }
    }

    /** {@code client -> server}: read-only pre-check, drives slot colours and the progress arrow. */
    public static final class CanUnlockRequest
            extends RpcApi.WithResponse<UnlockNodePayload, CheckResult, CanUnlockRequest> {
        public CanUnlockRequest() {
            super("tech_tree/can_unlock");
            executeOnServer();
            runOnMainThread();
        }

        @Override
        protected StreamCodec<? super FriendlyByteBuf, UnlockNodePayload> getRequestPayloadCodec() {
            return UnlockNodePayload.CODEC;
        }

        @Override
        protected StreamCodec<? super FriendlyByteBuf, CheckResult> getResponsePayloadCodec() {
            return CheckResult.CODEC;
        }

        @Override
        public CheckResult handleInstant(UnlockNodePayload request, IPayloadContext context) {
            if (context.player() instanceof net.minecraft.server.level.ServerPlayer player) {
                return ServerCheck.checkNode(player, request.location());
            }
            return CheckResult.empty();
        }

        public CompletableFuture<CheckResult> request(String location) {
            return run(new UnlockNodePayload(location));
        }
    }

    public record UnlockNodePayload(String location) {
        public static final StreamCodec<FriendlyByteBuf, UnlockNodePayload> CODEC = StreamCodec.of(
                (buf, value) -> buf.writeUtf(value.location()),
                buf -> new UnlockNodePayload(buf.readUtf()));
    }

    /** {@code client -> server}: "unlock this node for me" answered with the authoritative snapshot. */
    public static final class UnlockNodeRequest
            extends RpcApi.WithResponse<UnlockNodePayload, TreeSnapshot, UnlockNodeRequest> {
        public UnlockNodeRequest() {
            super("tech_tree/unlock_node");
            executeOnServer();
            runOnMainThread();
        }

        @Override
        protected StreamCodec<? super FriendlyByteBuf, UnlockNodePayload> getRequestPayloadCodec() {
            return UnlockNodePayload.CODEC;
        }

        @Override
        protected StreamCodec<? super FriendlyByteBuf, TreeSnapshot> getResponsePayloadCodec() {
            return TreeSnapshot.CODEC;
        }

        @Override
        public TreeSnapshot handleInstant(UnlockNodePayload request, IPayloadContext context) {
            if (context.player() instanceof net.minecraft.server.level.ServerPlayer player) {
                return ServerUnlock.applyNode(player, request.location());
            }
            throw new IllegalStateException("Tech tree unlock requested without a server player");
        }

        public CompletableFuture<TreeSnapshot> request(String location) {
            return run(new UnlockNodePayload(location));
        }
    }

    public record UnlockGroupPayload(String group) {
        public static final StreamCodec<FriendlyByteBuf, UnlockGroupPayload> CODEC = StreamCodec.of(
                (buf, value) -> buf.writeUtf(value.group()),
                buf -> new UnlockGroupPayload(buf.readUtf()));
    }

    /** {@code client -> server}: "publish this group on my board". */
    public static final class UnlockGroupRequest
            extends RpcApi.WithResponse<UnlockGroupPayload, TreeSnapshot, UnlockGroupRequest> {
        public UnlockGroupRequest() {
            super("tech_tree/unlock_group");
            executeOnServer();
            runOnMainThread();
        }

        @Override
        protected StreamCodec<? super FriendlyByteBuf, UnlockGroupPayload> getRequestPayloadCodec() {
            return UnlockGroupPayload.CODEC;
        }

        @Override
        protected StreamCodec<? super FriendlyByteBuf, TreeSnapshot> getResponsePayloadCodec() {
            return TreeSnapshot.CODEC;
        }

        @Override
        public TreeSnapshot handleInstant(UnlockGroupPayload request, IPayloadContext context) {
            if (context.player() instanceof net.minecraft.server.level.ServerPlayer player) {
                return ServerUnlock.applyGroup(player, request.group());
            }
            throw new IllegalStateException("Tech tree group unlock requested without a server player");
        }

        public CompletableFuture<TreeSnapshot> request(String group) {
            return run(new UnlockGroupPayload(group));
        }
    }
}
