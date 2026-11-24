package cn.kycraft.kuayue.parts.bogeys.core;

import com.simibubi.create.content.trains.bogey.BogeySizes;
import com.simibubi.create.content.trains.bogey.BogeyStyle;
import lib.kasuga.create.proxy.BogeyModels;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registers compiled data-driven bogey model plans and their shared placement entries.
 */
public final class KuaYueBogeyModels {
    private record Key(ResourceLocation style, ResourceLocation size) {
        private Key {
            Objects.requireNonNull(style, "style");
            Objects.requireNonNull(size, "size");
        }
    }

    private static final Map<Key, BogeyModelPlan> PLANS = new ConcurrentHashMap<>();

    private KuaYueBogeyModels() {
    }

    /**
     * Registers one style/size placement and retains its compiled motion plan.
     *
     * @param style committed Create style
     * @param size  registered Create size
     * @param plan  compiled data-driven model plan
     */
    public static void register(BogeyStyle style, BogeySizes.BogeySize size, BogeyModelPlan plan) {
        Objects.requireNonNull(style, "style");
        Objects.requireNonNull(size, "size");
        Objects.requireNonNull(plan, "plan");
        BogeyModels.register(style.id, size, plan.staticParts(), plan.yOffset());
        PLANS.put(new Key(style.id, size.id()), plan);
    }

    /**
     * Resolves a compiled plan for a style and size pair.
     *
     * @param styleId committed style ID
     * @param sizeId  registered size ID
     * @return the compiled plan, or {@code null} when this pair has no data-driven plan
     */
    public static @Nullable BogeyModelPlan resolve(
            ResourceLocation styleId,
            ResourceLocation sizeId
    ) {
        if (styleId == null || sizeId == null) {
            return null;
        }
        return PLANS.get(new Key(styleId, sizeId));
    }
}
