package cn.kycraft.kuayue.content.gameplay.food.fluid;

import cn.kycraft.kuayue.content.gameplay.food.FluidRegistration;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Vector3f;

/**
 * Client-only tint / fog / texture hooks for the drink fluid types.
 */
@EventBusSubscriber(modid = "kuayue", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class FluidClientRegistration {

    private FluidClientRegistration() {
    }

    @SubscribeEvent
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(extension(FluidRegistration.COLA_FLUID_TYPE.get()), FluidRegistration.COLA_FLUID_TYPE);
        event.registerFluidType(extension(FluidRegistration.BLUE_BULL_FLUID_TYPE.get()), FluidRegistration.BLUE_BULL_FLUID_TYPE);
    }

    private static IClientFluidTypeExtensions extension(KuayueFluidType type) {
        return new IClientFluidTypeExtensions() {
            @Override
            public int getTintColor() {
                return type.tintColor();
            }

            @Override
            public ResourceLocation getStillTexture() {
                return type.stillTexture();
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return type.flowingTexture();
            }

            @Override
            public ResourceLocation getOverlayTexture() {
                return type.overlayTexture();
            }

            @Override
            public Vector3f modifyFogColor(Camera camera, float partialTick, ClientLevel level,
                                           int renderDistance, float darkenWorldAmount, Vector3f fluidFogColor) {
                return type.fogColor();
            }

            @Override
            public void modifyFogRender(Camera camera, FogRenderer.FogMode mode, float renderDistance,
                                        float partialTick, float nearDistance, float farDistance, FogShape shape) {
                RenderSystem.setShaderFogStart(type.fogStart());
                RenderSystem.setShaderFogEnd(type.fogEnd());
            }
        };
    }
}
