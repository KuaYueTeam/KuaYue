package cn.kycraft.kuayue.overhead_line.block.decorating;

import cn.kycraft.kuayue.overhead_line.block.support.variants.ModelledOverheadSupportBlock;
import cn.kycraft.kuayue.overhead_line.block.support.variants.NormalOverheadSupportBlock;
import cn.kycraft.kuayue.overhead_line.block.support.variants.NormalOverheadSupportBlock.TrussShape;
import com.google.gson.JsonObject;
import lib.kasuga.registration.minecraft.block.BlockReg;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public final class AllOverheadDirectionalBlock {
    private static final String PARAM_SHAPE = "shape";

    private AllOverheadDirectionalBlock() {}

    public static BlockReg<NormalOverheadSupportBlock> createRegistration(String id, @Nullable JsonObject params) {
        return createRegistration(id, params, readShape(params));
    }

    public static BlockReg<NormalOverheadSupportBlock> createSupportRegistration(String id, @Nullable JsonObject params) {
        return createRegistration(id, params, null);
    }

    private static BlockReg<NormalOverheadSupportBlock> createRegistration(String id, @Nullable JsonObject params,
                                                                           @Nullable TrussShape shape) {
        ResourceLocation model = params == null || !params.has("model") || params.get("model").isJsonNull()
                ? null : ResourceLocation.parse(params.get("model").getAsString());
        int yaw = readInt(params, "model_yaw", 0);
        int yawStep = readInt(params, "model_yaw_step", 90);
        BlockReg<NormalOverheadSupportBlock> registration = BlockReg.of(id, properties -> {
            if (shape == null) properties.noCollission();
            return model == null ? new NormalOverheadSupportBlock(properties, shape)
                    : new ModelledOverheadSupportBlock(properties, shape, model, yaw, yawStep);
        });
        registration.withBlockItem(id, () -> BlockItem::new);
        return registration;
    }

    private static int readInt(@Nullable JsonObject params, String key, int fallback) {
        return params == null || !params.has(key) || params.get(key).isJsonNull()
                ? fallback : params.get(key).getAsInt();
    }

    private static TrussShape readShape(@Nullable JsonObject params) {
        if (params == null || !params.has(PARAM_SHAPE) || params.get(PARAM_SHAPE).isJsonNull()) {
            return TrussShape.SMALL;
        }
        return switch (params.get(PARAM_SHAPE).getAsString().toLowerCase(Locale.ROOT)) {
            case "big" -> TrussShape.BIG;
            case "pillar" -> TrussShape.PILLAR;
            default -> TrussShape.SMALL;
        };
    }
}
