package cn.kycraft.kuayue.parts.core.factory;

import cn.kycraft.kuayue.packs.shapes.KuaYueShapeBindingSpec;
import cn.kycraft.kuayue.parts.core.panel.TrainPanelBlock;
import lib.kasuga.registration.minecraft.block.BlockReg;
import lib.kasuga.registration.minecraft.block.BlockRegModifiers;
import lib.kasuga.registration.minecraft.block.shape.BlockShapeBindings;
import lib.kasuga.registration.minecraft.block.shape.BlockShapePlan;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Function;

/**
 * Block registration that resolves vehicle bindings after the factory has created the real state
 * definition, then installs the immutable lib plan before Minecraft initializes state caches.
 */
public final class KuaYueBlockReg<T extends Block> extends BlockReg<T> {
    private final Function<BlockReg<T>, Function<BlockBehaviour.Properties, T>> blockSupplier;

    private KuaYueBlockReg(
            String name,
            Function<BlockReg<T>, Function<BlockBehaviour.Properties, T>> supplier
    ) {
        super(name, supplier);
        this.blockSupplier = supplier;
    }

    public static <T extends Block> KuaYueBlockReg<T> of(
            String name,
            Function<BlockBehaviour.Properties, T> supplier
    ) {
        return new KuaYueBlockReg<>(name, ignored -> supplier);
    }

    @Override
    protected T createObject(ResourceLocation id) {
        BlockBehaviour.Properties properties =
                transform(BlockRegModifiers.TYPE_BLOCK_PROPERTIES, BlockBehaviour.Properties.of());
        properties = applyProperties(BlockBehaviour.Properties.class, properties);
        T block = blockSupplier.apply(this).apply(properties);

        KuaYueShapeBindingSpec spec =
                applyProperties(KuaYueShapeBindingSpec.class, KuaYueShapeBindingSpec.NONE);
        BlockShapePlan plan = spec.isEmpty()
                ? applyProperties(BlockShapePlan.class, BlockShapePlan.empty())
                : spec.buildPlan(block);
        BlockShapeBindings.install(
                block,
                plan,
                state -> block instanceof TrainPanelBlock panel
                        && !panel.multiCellFootprint().relativeCells(state).isEmpty()
        );
        if (!spec.isEmpty() && block instanceof TrainPanelBlock panel) {
            panel.configureShapeCoverage(true);
        }
        return block;
    }
}
