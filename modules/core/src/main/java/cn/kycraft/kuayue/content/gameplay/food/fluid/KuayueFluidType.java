package cn.kycraft.kuayue.content.gameplay.food.fluid;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidType;
import org.joml.Vector3f;

/**
 * Shared {@link FluidType} for the KuaYue drinks.
 *
 * <p>Re-uses vanilla water still / flowing textures and tints them with the drink colour, the same
 * approach the 1.0 {@code FluidReg} took. The client-side extension (tint, fog colour, fog range)
 * is registered from {@code FluidClientRegistration} behind a dist guard.</p>
 */
public class KuayueFluidType extends FluidType {

    public static final ResourceLocation WATER_STILL = ResourceLocation.fromNamespaceAndPath("kuayue", "block/water_still");
    public static final ResourceLocation WATER_FLOW = ResourceLocation.fromNamespaceAndPath("kuayue", "block/water_flow");

    private final int tintColor;
    private final ResourceLocation stillTexture;
    private final ResourceLocation flowingTexture;
    private final ResourceLocation overlayTexture;
    private final Vector3f fogColor;
    private final float fogStart;
    private final float fogEnd;

    public KuayueFluidType(Properties properties, ResourceLocation stillTexture, ResourceLocation flowingTexture,
                           ResourceLocation overlayTexture, int tintColor, Vector3f fogColor,
                           float fogStart, float fogEnd) {
        super(properties);
        this.stillTexture = stillTexture;
        this.flowingTexture = flowingTexture;
        this.overlayTexture = overlayTexture;
        this.tintColor = tintColor;
        this.fogColor = fogColor;
        this.fogStart = fogStart;
        this.fogEnd = fogEnd;
    }

    public static Properties drinkProperties(int density, int viscosity, int lightLevel) {
        return Properties.create()
                .canExtinguish(true)
                .supportsBoating(true)
                .density(density)
                .viscosity(viscosity)
                .lightLevel(lightLevel)
                .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH);
    }

    public int tintColor() {
        return tintColor;
    }

    public ResourceLocation stillTexture() {
        return stillTexture;
    }

    public ResourceLocation flowingTexture() {
        return flowingTexture;
    }

    public ResourceLocation overlayTexture() {
        return overlayTexture;
    }

    public Vector3f fogColor() {
        return fogColor;
    }

    public float fogStart() {
        return fogStart;
    }

    public float fogEnd() {
        return fogEnd;
    }

    /** Converts a {@code 0xAARRGGBB} colour into a normalised RGB vector. */
    protected static Vector3f rgb(int argb) {
        return new Vector3f(((argb >> 16) & 0xFF) / 255F, ((argb >> 8) & 0xFF) / 255F, (argb & 0xFF) / 255F);
    }
}
