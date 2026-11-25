package cn.kycraft.kuayue.overhead_line.block.line;

import cn.kycraft.kuayue.KuaYue;
import cn.kycraft.kuayue.overhead_line.wire.OverheadLineEndpoint;
import cn.kycraft.kuayue.overhead_line.wire.OverheadLineWireKind;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public record OverheadLineConnection(
        OverheadLineEndpoint from,
        OverheadLineEndpoint to,
        OverheadLineWireKind kind
) implements CustomPacketPayload {
    public static final Type<OverheadLineConnection> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "overhead_line_add"));

    public OverheadLineConnection{
        if (!from.dimension().equals(to.dimension())){throw new IllegalArgumentException("接触网端点不能跨纬度");}
        int order = Long.compare(from.blockPos().asLong(),to.blockPos().asLong());
        if (order == 0){order = Integer.compare(from.pointIndex(),to.pointIndex());}
        if (order > 0){
            var temp = from;
            from = to;
            to = temp;
        }
    }

    public static final StreamCodec<RegistryFriendlyByteBuf , OverheadLineConnection> STREAM_CODEC = new StreamCodec<RegistryFriendlyByteBuf, OverheadLineConnection>() {
        @Override
        public OverheadLineConnection decode(RegistryFriendlyByteBuf registryFriendlyByteBuf) {
            ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, registryFriendlyByteBuf.readResourceLocation());
            var from = new OverheadLineEndpoint(dimension,registryFriendlyByteBuf.readBlockPos(),registryFriendlyByteBuf.readVarInt());
            var to = new OverheadLineEndpoint(dimension,registryFriendlyByteBuf.readBlockPos(),registryFriendlyByteBuf.readerIndex());
            OverheadLineWireKind kind = switch (registryFriendlyByteBuf.readVarInt()){
                case 0 -> OverheadLineWireKind.OVERHEAD_LINE_WIRE;
                case 1 -> OverheadLineWireKind.ELECTRONIC_WIRE;
                default -> throw new IllegalArgumentException("未知线材");
            };
            return new OverheadLineConnection(from,to,kind);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, OverheadLineConnection overheadLineConnection) {
            buf.writeResourceLocation(overheadLineConnection.from().dimension().location());
            buf.writeBlockPos(overheadLineConnection.from().blockPos());
            buf.writeVarInt(overheadLineConnection.from().pointIndex());
            buf.writeVarInt(switch (overheadLineConnection.kind()){
                case ELECTRONIC_WIRE -> 1;
                case OVERHEAD_LINE_WIRE -> 0;
            });
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return null;
    }
}
