package cn.kycraft.kuayue.content.gameplay.food.fluid;

import net.minecraft.resources.ResourceLocation;

/** Fluid type for 蓝牛: tint {@code #EEDD82}, fog {@code (238,221,130)}, density 2 / viscosity 2. */
public class BlueBullFluidType extends KuayueFluidType {

    public static final int TINT = 0xFFEEDD82;
    public static final int FOG_R = 238;
    public static final int FOG_G = 221;
    public static final int FOG_B = 130;

    public BlueBullFluidType() {
        super(drinkProperties(2, 2, 5),
                WATER_STILL,
                WATER_FLOW,
                ResourceLocation.fromNamespaceAndPath("kuayue", "block/fluid/blue_bull_fluid"),
                TINT,
                rgb(TINT),
                1f,
                6f);
    }
}
