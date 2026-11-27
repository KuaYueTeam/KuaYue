package cn.kycraft.kuayue.parts.core.panel.registration;

import cn.kycraft.kuayue.parts.core.panel.SkirtBlock;
import cn.kycraft.kuayue.parts.core.panel.SkirtBlockItem;
import cn.kycraft.kuayue.parts.core.factory.KuaYueBlockReg;
import cn.kycraft.kuayue.parts.core.multiblock.MultiCellFootprint;
import com.google.gson.JsonObject;
import lib.kasuga.registration.Reg;
import lib.kasuga.registration.minecraft.block.BlockReg;
import lib.kasuga.registration.minecraft.block.BlockRegConfigurations;
import lib.kasuga.registration.minecraft.item.ItemReg;
import lib.kasuga.registration.minecraft.item.ItemRegConfigurations;
import net.minecraft.world.flag.FeatureFlag;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Function;

public class SkirtReg<T extends SkirtBlock> extends Reg<SkirtReg<T>, Block> implements BlockRegConfigurations<SkirtReg<T>>, ItemRegConfigurations<SkirtReg<T>> {
    private final BlockReg<T> blockReg;
    private final ItemReg<SkirtBlockItem> itemReg;

    public SkirtReg(String name, Function<BlockBehaviour.Properties, T> blockSupplier) {
        this.blockReg = BlockReg.of(name, blockSupplier).setParent(this);
        this.itemReg = ItemReg.of(name, (i)->new SkirtBlockItem(blockReg.getEntry(), i)).setParent(this);
    }

    public SkirtReg(
            String name,
            JsonObject params,
            Function<BlockBehaviour.Properties, T> blockSupplier
    ) {
        Function<BlockBehaviour.Properties, T> configured = properties -> {
            T block = blockSupplier.apply(properties);
            block.configureMultiCell(MultiCellFootprint.parse(params));
            return block;
        };
        BlockReg<T> registration = params != null && params.has("shape_bindings")
                ? KuaYueBlockReg.of(name, configured)
                : BlockReg.of(name, configured);
        this.blockReg = registration.setParent(this);
        this.itemReg = ItemReg.of(name, (i)->new SkirtBlockItem(blockReg.getEntry(), i)).setParent(this);
    }

    @Override
    public Block getEntry() {
        return blockReg.getEntry();
    }

    @Override
    public SkirtReg requiredFeatures(FeatureFlag[] requiredFeatures) {
        return BlockRegConfigurations.super.requiredFeatures(requiredFeatures);
    }

    public Item getItem() {
        return itemReg.getEntry();
    }
}
