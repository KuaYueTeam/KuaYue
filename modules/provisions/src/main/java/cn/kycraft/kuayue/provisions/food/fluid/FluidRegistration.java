package cn.kycraft.kuayue.provisions.food.fluid;

import cn.kycraft.kuayue.KuaYue;
import io.micronaut.context.annotation.Context;
import lib.kasuga.registration.RegistryGroup;
import lib.kasuga.registration.minecraft.fluid.FluidBlockReg;
import lib.kasuga.registration.minecraft.fluid.FluidReg;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

/**
 * 可乐 / 蓝牛流体、流体类型、放置方块与桶。
 *
 * <p>全部走 KasugaLib 的 {@link FluidReg}：流体类型、Source/Flowing 两个 fluid、液面方块和桶
 * 由一个注册项统一管理。客户端渲染数据（贴图、tint、雾）配置在 {@code FluidTypeReg} 上，由
 * Kasuga 通过 {@code RegisterClientExtensionsEvent} 下发，因此这里不再需要手写的
 * {@code IClientFluidTypeExtensions}。</p>
 *
 * <p>数值沿用 1.0：可乐 密度 1 / 黏度 8，蓝牛 密度 2 / 黏度 2，两者 lightLevel 均为 5；
 * 流动参数 slopeFindDistance 2 / levelDecreasePerBlock 2。</p>
 */
@Context
public class FluidRegistration {

    public static final RegistryGroup GROUP_FLUID = new RegistryGroup()
            .setParent(KuaYue.REGISTRY);

    // ------------------------------------------------------------------ 可乐

    public static final FluidReg<BaseFlowingFluid.Source, BaseFlowingFluid.Flowing> COLA =
            new FluidReg<>(
                    "cola_fluid", "cola_still_fluid", "cola_flowing_fluid")
                    .instances(BaseFlowingFluid.Source::new, BaseFlowingFluid.Flowing::new)
                    .slopeFindDistance(2)
                    .levelDecreasePerBlock(2)
                    .type(t -> t.tint(0xFFD2691E)
                            .fogColor(210 / 255f, 105 / 255f, 30 / 255f)
                            .fog(1.0f, 6.0f)
                            .density(1)
                            .viscosity(8)
                            .lightLevel(5)
                            .supportsBoating(true)
                            .still("block/water_still")
                            .flowing("block/water_flow")
                            .overlay("block/fluid/cola_fluid")
                            .defaultBucketSounds()
                            .vaporizeSound(SoundEvents.FIRE_EXTINGUISH))
                    .block(FluidBlockReg.of("cola_block",
                            fluid -> props -> new ColaBlock(fluid, waterLike(props))))
                    .bucket("cola_fluid_bucket")
                    .setParent(GROUP_FLUID);

    // ------------------------------------------------------------------ 蓝牛

    public static final FluidReg<BaseFlowingFluid.Source, BaseFlowingFluid.Flowing> BLUE_BULL =
            new FluidReg<>(
                    "blue_bull_fluid", "blue_bull_still_fluid", "blue_bull_flowing_fluid")
                    .instances(BaseFlowingFluid.Source::new, BaseFlowingFluid.Flowing::new)
                    .slopeFindDistance(2)
                    .levelDecreasePerBlock(2)
                    .type(t -> t.tint(0xFFEEDD82)
                            .fogColor(238 / 255f, 221 / 255f, 130 / 255f)
                            .fog(1.0f, 6.0f)
                            .density(2)
                            .viscosity(2)
                            .lightLevel(5)
                            .supportsBoating(true)
                            .still("block/water_still")
                            .flowing("block/water_flow")
                            .overlay("block/fluid/blue_bull_fluid")
                            .defaultBucketSounds()
                            .vaporizeSound(SoundEvents.FIRE_EXTINGUISH))
                    .block(FluidBlockReg.of("blue_bull_block",
                            fluid -> props -> new BlueBullBlock(fluid, waterLike(props))))
                    .bucket("blue_bull_fluid_bucket")
                    .setParent(GROUP_FLUID);

    /** 液面方块沿用水的属性（无碰撞、可替换、不会被方块实体阻挡）。 */
    private static BlockBehaviour.Properties waterLike(BlockBehaviour.Properties properties) {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.WATER);
    }
}
