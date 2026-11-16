package cn.kycraft.kuayue.packs.network;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Configuration-phase payload types for the pack-set gate. */
public final class PackSetPayloads {
    public static final CustomPacketPayload.Type<Query> QUERY_TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                    "kuayue",
                    "configuration/pack_set/query"
            ));
    public static final CustomPacketPayload.Type<Declaration> DECLARATION_TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                    "kuayue",
                    "configuration/pack_set/declaration"
            ));

    private PackSetPayloads() {
    }

    public record Query() implements CustomPacketPayload {
        public static final Query INSTANCE = new Query();
        public static final StreamCodec<io.netty.buffer.ByteBuf, Query> STREAM_CODEC =
                StreamCodec.unit(INSTANCE);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return QUERY_TYPE;
        }
    }

    public record Declaration(PackSetDeclaration declaration) implements CustomPacketPayload {
        public static final StreamCodec<io.netty.buffer.ByteBuf, Declaration> STREAM_CODEC =
                PackSetDeclarationCodec.STREAM_CODEC.map(Declaration::new, Declaration::declaration);

        public Declaration {
            if (declaration == null) throw new NullPointerException("declaration");
        }

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return DECLARATION_TYPE;
        }
    }
}
