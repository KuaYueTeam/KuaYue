package cn.kycraft.kuayue.content.gameplay.food.fluid;

import net.minecraft.resources.ResourceLocation;

/** Fluid type for 可乐: tint {@code #D2691E}, fog {@code (210,105,30)}, density 1 / viscosity 8. */
public class ColaFluidType extends KuayueFluidType {

    public static final int TINT = 0xFFD2691E;
    public static final int FOG_R = 210;
    public static final int FOG_G = 105;
    public static final int FOG_B = 30;

    public ColaFluidType() {
        super(drinkProperties(1, 8, 5),
                WATER_STILL,
                WATER_FLOW,
                ResourceLocation.fromNamespaceAndPath("kuayue", "block/fluid/cola_fluid"),
                TINT,
                rgb(TINT),
                1f,
                6f);
    }
}
