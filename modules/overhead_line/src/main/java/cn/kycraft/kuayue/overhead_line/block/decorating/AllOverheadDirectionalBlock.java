package cn.kycraft.kuayue.overhead_line.block.decorating;

import cn.kycraft.kuayue.overhead_line.block.support.variants.ModelledOverheadSupportBlock;
import cn.kycraft.kuayue.overhead_line.block.support.variants.NormalOverheadSupportBlock;
import cn.kycraft.kuayue.overhead_line.block.support.variants.NormalOverheadSupportBlock.TrussShape;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lib.kasuga.registration.minecraft.block.BlockReg;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class AllOverheadDirectionalBlock {
    private static final String PARAM_SHAPE = "shape";

    private AllOverheadDirectionalBlock() {}

    public static BlockReg<NormalOverheadSupportBlock> createRegistration(String id, @Nullable JsonObject params) {
        return createRegistration(id, params, readShape(params),List.of());
    }

    public static BlockReg<NormalOverheadSupportBlock> createSupportRegistration(String id, @Nullable JsonObject params) {
        List<Vec3> connectionsPoints = readConnectionsPoints(id,params);
        return createRegistration(id, params, TrussShape.FULL_BLOCK, connectionsPoints);
    }

    private static BlockReg<NormalOverheadSupportBlock> createRegistration(String id, @Nullable JsonObject params, TrussShape shape , List<Vec3> connectionsPoints) {
        ResourceLocation model = params == null || !params.has("model") || params.get("model").isJsonNull() ? null : ResourceLocation.parse(params.get("model").getAsString());
        int yaw = readInt(params, "model_yaw", 0);
        int yawStep = readInt(params, "model_yaw_step", 90);
        BlockReg<NormalOverheadSupportBlock> registration = BlockReg.of(id, properties -> {
            return model == null ? new NormalOverheadSupportBlock(properties, shape, connectionsPoints) : new ModelledOverheadSupportBlock(properties, shape, model, yaw, yawStep,connectionsPoints);
        });
        registration.withBlockItem(id, () -> BlockItem::new);
        return registration;
    }

    private static int readInt(@Nullable JsonObject params, String key, int fallback) {
        return params == null || !params.has(key) || params.get(key).isJsonNull()
                ? fallback : params.get(key).getAsInt();
    }

    private static List<Vec3> readConnectionsPoints(String id, @Nullable JsonObject params){
        if (params == null || !params.has("connection_points")){return List.of();}
        JsonElement value = params.get("connection_points");
        if (!value.isJsonArray()){throw new IllegalArgumentException(id + "的connection_points必须是挂点数组哦");}
        JsonArray array = value.getAsJsonArray();
        List<Vec3> points = new ArrayList<>(array.size());
        for (int i = 0 ; i < array.size() ; i++){
            JsonElement entry = array.get(i);
            String path = id + "的connection_points[" + i + "]";
            if (!entry.isJsonArray() || entry.getAsJsonArray().size() != 3){throw new IllegalArgumentException(path + "必须是包含三个坐标的数组[x,y,z]哦");}
            JsonArray point = entry.getAsJsonArray();
            double x = readFiniteCoordinate(point.get(0), path + ".x");
            double y = readFiniteCoordinate(point.get(1), path + ".y");
            double z = readFiniteCoordinate(point.get(2), path + ".z");
            points.add(new Vec3(x,y,z));
        }
        return List.copyOf(points);
    }

    private static double readFiniteCoordinate(JsonElement value , String path){
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()){throw new IllegalArgumentException(path + "必须是数字哦");}
        double number = value.getAsDouble();
        if (!Double.isFinite(number)){throw new IllegalArgumentException(path + "必须是有限数字哦");}
        return number;
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
