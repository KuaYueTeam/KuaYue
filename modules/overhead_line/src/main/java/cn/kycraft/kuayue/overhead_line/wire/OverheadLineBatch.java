package cn.kycraft.kuayue.overhead_line.wire;

import cn.kycraft.kuayue.KuaYue;
import cn.kycraft.kuayue.overhead_line.block.line.OverheadLineConnection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

record OverheadLineBatch(Action action, List<OverheadLineConnection> links) implements CustomPacketPayload {
    static final int MAX_LINKS = 512;
    static final Type<OverheadLineBatch> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "overhead_line_batch"));

    enum Action { RESET, APPEND, REMOVE }

    OverheadLineBatch {
        links = List.copyOf(links);
        if (links.size() > MAX_LINKS || (action == Action.RESET && !links.isEmpty())) {
            throw new IllegalArgumentException("Invalid overhead line batch size");
        }
    }

    static final StreamCodec<RegistryFriendlyByteBuf, OverheadLineBatch> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public OverheadLineBatch decode(RegistryFriendlyByteBuf buf) {
            var action = buf.readEnum(Action.class);
            int size = buf.readVarInt();
            if (size < 0 || size > MAX_LINKS || (action == Action.RESET && size != 0)) {
                throw new IllegalArgumentException("Invalid overhead line batch size: " + size);
            }
            var links = new ArrayList<OverheadLineConnection>(size);
            for (int i = 0; i < size; i++) links.add(OverheadLineConnection.STREAM_CODEC.decode(buf));
            return new OverheadLineBatch(action, links);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, OverheadLineBatch batch) {
            buf.writeEnum(batch.action());
            buf.writeVarInt(batch.links().size());
            for (var link : batch.links()) OverheadLineConnection.STREAM_CODEC.encode(buf, link);
        }
    };

    @Override
    public Type<OverheadLineBatch> type() {
        return TYPE;
    }
}
