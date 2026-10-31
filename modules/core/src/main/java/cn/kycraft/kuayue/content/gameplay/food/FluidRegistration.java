package cn.kycraft.kuayue.content.gameplay.food;

import cn.kycraft.kuayue.KuaYue;
import cn.kycraft.kuayue.content.gameplay.food.fluid.*;
import io.micronaut.context.annotation.Context;
import lib.kasuga.registration.RegistryGroup;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * 可乐 / 蓝牛 fluids, fluid types, source blocks and buckets.
 *
 * <p>Ported from the 1.0 {@code AllFluids} / {@code FluidsInit} / {@code FluidTypesInit} trio.
 * KasugaLibNext has no fluid registrar, so entries go through plain NeoForge
 * {@link DeferredRegister}s hung under the mod's Kasuga registry tree.</p>
 */
@Context
public class FluidRegistration {

    public static final RegistryGroup GROUP_FLUID = new RegistryGroup()
            .setParent(KuaYue.REGISTRY);

    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, KuaYue.MODID);
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, KuaYue.MODID);
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, KuaYue.MODID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, KuaYue.MODID);

    // ------------------------------------------------------------------ 可乐

    public static final DeferredHolder<FluidType, ColaFluidType> COLA_FLUID_TYPE =
            FLUID_TYPES.register("cola_fluid", ColaFluidType::new);

    public static final BaseFlowingFluid.Properties COLA_PROPERTIES = new BaseFlowingFluid.Properties(
            COLA_FLUID_TYPE, Fluids.COLA_STILL, Fluids.COLA_FLOWING)
            .block(Blocks.COLA_BLOCK)
            .bucket(Items.COLA_BUCKET)
            .slopeFindDistance(2)
            .levelDecreasePerBlock(2);

    // ------------------------------------------------------------------ 蓝牛

    public static final DeferredHolder<FluidType, BlueBullFluidType> BLUE_BULL_FLUID_TYPE =
            FLUID_TYPES.register("blue_bull_fluid", BlueBullFluidType::new);

    public static final BaseFlowingFluid.Properties BLUE_BULL_PROPERTIES = new BaseFlowingFluid.Properties(
            BLUE_BULL_FLUID_TYPE, Fluids.BLUE_BULL_STILL, Fluids.BLUE_BULL_FLOWING)
            .block(Blocks.BLUE_BULL_BLOCK)
            .bucket(Items.BLUE_BULL_BUCKET)
            .slopeFindDistance(2)
            .levelDecreasePerBlock(2);

    public static void register(IEventBus modEventBus) {
        FLUID_TYPES.register(modEventBus);
        FLUIDS.register(modEventBus);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
    }

    /** Fluid entries, declared after the properties they reference (no forward references). */
    public static final class Fluids {
        public static final DeferredHolder<Fluid, ColaFluid.Source> COLA_STILL =
                FLUIDS.register("cola_still_fluid", () -> new ColaFluid.Source(COLA_PROPERTIES));
        public static final DeferredHolder<Fluid, ColaFluid.Flowing> COLA_FLOWING =
                FLUIDS.register("cola_flowing_fluid", () -> new ColaFluid.Flowing(COLA_PROPERTIES));

        public static final DeferredHolder<Fluid, BlueBullFluid.Source> BLUE_BULL_STILL =
                FLUIDS.register("blue_bull_still_fluid", () -> new BlueBullFluid.Source(BLUE_BULL_PROPERTIES));
        public static final DeferredHolder<Fluid, BlueBullFluid.Flowing> BLUE_BULL_FLOWING =
                FLUIDS.register("blue_bull_flowing_fluid", () -> new BlueBullFluid.Flowing(BLUE_BULL_PROPERTIES));
    }

    /** Source blocks. */
    public static final class Blocks {
        public static final DeferredHolder<Block, ColaBlock> COLA_BLOCK =
                BLOCKS.register("cola_block", () -> new ColaBlock(Fluids.COLA_STILL.get(),
                        BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.WATER)));

        public static final DeferredHolder<Block, BlueBullBlock> BLUE_BULL_BLOCK =
                BLOCKS.register("blue_bull_block", () -> new BlueBullBlock(Fluids.BLUE_BULL_STILL.get(),
                        BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.WATER)));
    }

    /** Buckets. 蓝牛 is {@code stacksTo(1)} as specified; both return the empty bucket. */
    public static final class Items {
        public static final DeferredHolder<Item, BucketItem> COLA_BUCKET =
                ITEMS.register("cola_fluid_bucket", () -> new BucketItem(Fluids.COLA_STILL.get(),
                        new Item.Properties().stacksTo(1).craftRemainder(net.minecraft.world.item.Items.BUCKET)));

        public static final DeferredHolder<Item, BucketItem> BLUE_BULL_BUCKET =
                ITEMS.register("blue_bull_fluid_bucket", () -> new BucketItem(Fluids.BLUE_BULL_STILL.get(),
                        new Item.Properties().stacksTo(1).craftRemainder(net.minecraft.world.item.Items.BUCKET)));
    }
}
