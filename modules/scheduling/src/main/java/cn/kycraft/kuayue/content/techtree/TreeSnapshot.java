package cn.kycraft.kuayue.content.techtree;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Full client-side view of the tech tree for one player.
 *
 * <p>It replaces the reference implementation's handshake/batch/EOF lockstep transfer: the whole
 * tree for one player fits in a single RPC response, so there is no transmission state to stall on.</p>
 */
public final class TreeSnapshot {

    private static final StreamCodec<RegistryFriendlyByteBuf, ItemStack> ITEM_STACK_STREAM_CODEC =
            ItemStack.STREAM_CODEC.cast();


    public record NodeView(NodeLocation location, NodeType type, String nameKey, String descriptionKey,
                           String unlockKey, int exp, int level, ItemStack logo,
                           List<ItemStack> consume, List<ItemStack> blueprints, List<ItemStack> rewards,
                           List<NodeLocation> next, List<NodeLocation> prev, boolean hidden) {
    }

    public record GroupView(ResourceLocation id, ResourceLocation cover, String nameKey, String descriptionKey,
                            ItemStack icon, NodeLocation root, List<NodeLocation> incoming,
                            int totalNodes, int requiredPercentage) {
    }

    public final Map<NodeLocation, NodeView> nodes = new LinkedHashMap<>();
    public final Map<ResourceLocation, GroupView> groups = new LinkedHashMap<>();
    public final Map<ResourceLocation, Map<NodeLocation, int[]>> layout = new LinkedHashMap<>();
    public final UnlockState progress = new UnlockState();

    public static final StreamCodec<FriendlyByteBuf, TreeSnapshot> CODEC = StreamCodec.of(
            (buf, snapshot) -> {
                if (!(buf instanceof RegistryFriendlyByteBuf registry)) {
                    throw new IllegalArgumentException("Tech tree snapshot requires a registry buffer");
                }
                snapshot.write(registry);
            },
            buf -> {
                if (!(buf instanceof RegistryFriendlyByteBuf registry)) {
                    throw new IllegalArgumentException("Tech tree snapshot requires a registry buffer");
                }
                return read(registry);
            });

    /** Builds the snapshot for the given player from the linked server-side graph. */
    public static TreeSnapshot build(ServerPlayer player) {
        TreeSnapshot snapshot = new TreeSnapshot();
        snapshot.progress.copyFrom(PlayerProgress.of(player));

        TechTreeManager.get().groups().forEach((id, group) -> {
            snapshot.groups.put(id, new GroupView(id, group.cover, group.nameKey, group.descriptionKey,
                    group.icon.copy(), group.root() == null ? null : group.root().location,
                    new ArrayList<>(group.incoming()), group.nodes().size(), group.data.requiredPercentage));
            snapshot.layout.put(id, group.layout());
            group.nodes().forEach((location, node) -> snapshot.nodes.put(location, new NodeView(
                    location, node.type(), node.data.nameKey, node.data.descriptionKey,
                    node.data.unlockDescriptionKey, node.exp(), node.level(), node.logo.copy(),
                    new ArrayList<>(node.consume().keySet()),
                    new ArrayList<>(node.blueprints().keySet()),
                    new ArrayList<>(node.rewards().keySet()),
                    new ArrayList<>(node.data.nextNodes), new ArrayList<>(node.data.prevNodes),
                    node.hidden())));
        });
        return snapshot;
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(groups.size());
        groups.forEach((id, group) -> {
            buf.writeResourceLocation(id);
            boolean hasCover = group.cover() != null;
            buf.writeBoolean(hasCover);
            if (hasCover) buf.writeResourceLocation(group.cover());
            buf.writeUtf(group.nameKey());
            buf.writeUtf(group.descriptionKey());
            writeStack(buf, group.icon());
            boolean hasRoot = group.root() != null;
            buf.writeBoolean(hasRoot);
            if (hasRoot) group.root().write(buf);
            buf.writeVarInt(group.incoming().size());
            group.incoming().forEach(location -> location.write(buf));
            buf.writeVarInt(group.totalNodes());
            buf.writeVarInt(group.requiredPercentage());

            Map<NodeLocation, int[]> positions = layout.getOrDefault(id, Map.of());
            buf.writeVarInt(positions.size());
            positions.forEach((location, position) -> {
                location.write(buf);
                buf.writeVarInt(position[0]);
                buf.writeVarInt(position[1]);
            });
        });

        buf.writeVarInt(nodes.size());
        nodes.values().forEach(node -> {
            node.location().write(buf);
            node.type().write(buf);
            buf.writeUtf(node.nameKey());
            buf.writeUtf(node.descriptionKey());
            buf.writeUtf(node.unlockKey());
            buf.writeVarInt(node.exp());
            buf.writeVarInt(node.level());
            writeStack(buf, node.logo());
            writeStacks(buf, node.consume());
            writeStacks(buf, node.blueprints());
            writeStacks(buf, node.rewards());
            buf.writeVarInt(node.next().size());
            node.next().forEach(next -> next.write(buf));
            buf.writeVarInt(node.prev().size());
            node.prev().forEach(prev -> prev.write(buf));
            buf.writeBoolean(node.hidden());
        });

        progress.writeNetwork(buf);
    }

    private static TreeSnapshot read(RegistryFriendlyByteBuf buf) {
        TreeSnapshot snapshot = new TreeSnapshot();
        int groupCount = buf.readVarInt();
        for (int i = 0; i < groupCount; i++) {
            ResourceLocation id = buf.readResourceLocation();
            ResourceLocation cover = buf.readBoolean() ? buf.readResourceLocation() : null;
            String nameKey = buf.readUtf();
            String descriptionKey = buf.readUtf();
            ItemStack icon = readStack(buf);
            NodeLocation root = buf.readBoolean() ? NodeLocation.read(buf) : null;
            int incomingSize = buf.readVarInt();
            List<NodeLocation> incoming = new ArrayList<>(incomingSize);
            for (int j = 0; j < incomingSize; j++) incoming.add(NodeLocation.read(buf));
            int totalNodes = buf.readVarInt();
            int requiredPercentage = buf.readVarInt();
            snapshot.groups.put(id, new GroupView(id, cover, nameKey, descriptionKey, icon, root, incoming,
                    totalNodes, requiredPercentage));

            int positionCount = buf.readVarInt();
            Map<NodeLocation, int[]> positions = new LinkedHashMap<>();
            for (int j = 0; j < positionCount; j++) {
                NodeLocation location = NodeLocation.read(buf);
                positions.put(location, new int[]{buf.readVarInt(), buf.readVarInt()});
            }
            snapshot.layout.put(id, positions);
        }

        int nodeCount = buf.readVarInt();
        for (int i = 0; i < nodeCount; i++) {
            NodeLocation location = NodeLocation.read(buf);
            NodeType type = NodeType.read(buf);
            String nameKey = buf.readUtf();
            String descriptionKey = buf.readUtf();
            String unlockKey = buf.readUtf();
            int exp = buf.readVarInt();
            int level = buf.readVarInt();
            ItemStack logo = readStack(buf);
            List<ItemStack> consume = readStacks(buf);
            List<ItemStack> blueprints = readStacks(buf);
            List<ItemStack> rewards = readStacks(buf);
            int nextSize = buf.readVarInt();
            List<NodeLocation> next = new ArrayList<>(nextSize);
            for (int j = 0; j < nextSize; j++) next.add(NodeLocation.read(buf));
            int prevSize = buf.readVarInt();
            List<NodeLocation> prev = new ArrayList<>(prevSize);
            for (int j = 0; j < prevSize; j++) prev.add(NodeLocation.read(buf));
            boolean hidden = buf.readBoolean();
            snapshot.nodes.put(location, new NodeView(location, type, nameKey, descriptionKey, unlockKey,
                    exp, level, logo, consume, blueprints, rewards, next, prev, hidden));
        }

        snapshot.progress.readNetwork(buf);
        return snapshot;
    }

    /** Writes a possibly-empty stack: the vanilla ItemStack codec rejects EMPTY outright. */
    private static void writeStack(RegistryFriendlyByteBuf buf, ItemStack stack) {
        boolean present = stack != null && !stack.isEmpty();
        buf.writeBoolean(present);
        if (present) {
            ITEM_STACK_STREAM_CODEC.encode(buf, stack);
        }
    }

    private static ItemStack readStack(RegistryFriendlyByteBuf buf) {
        return buf.readBoolean() ? ITEM_STACK_STREAM_CODEC.decode(buf) : ItemStack.EMPTY;
    }

    private static void writeStacks(RegistryFriendlyByteBuf buf, List<ItemStack> stacks) {
        List<ItemStack> present = new ArrayList<>(stacks.size());
        for (ItemStack stack : stacks) {
            if (stack != null && !stack.isEmpty()) present.add(stack);
        }
        buf.writeVarInt(present.size());
        present.forEach(stack -> ITEM_STACK_STREAM_CODEC.encode(buf, stack));
    }

    private static List<ItemStack> readStacks(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<ItemStack> stacks = new ArrayList<>(size);
        for (int i = 0; i < size; i++) stacks.add(ITEM_STACK_STREAM_CODEC.decode(buf));
        return stacks;
    }
}
