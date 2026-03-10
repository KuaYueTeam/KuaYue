package cn.kycraft.kuayue.parts.core.panel.registration;

import cn.kycraft.kuayue.parts.core.panel.PanelBlockItem;
import cn.kycraft.kuayue.parts.core.panel.TrainPanelBlock;
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

public class PanelReg<T extends TrainPanelBlock> extends Reg<PanelReg<T>, Block> implements BlockRegConfigurations<PanelReg<T>>, ItemRegConfigurations<PanelReg<T>> {
    private final BlockReg<T> blockReg;
    private final ItemReg<PanelBlockItem> itemReg;

    public PanelReg(String name, Function<BlockBehaviour.Properties, T> blockSupplier) {
        this.blockReg = BlockReg.of(name, blockSupplier).setParent(this);
        this.itemReg = ItemReg.of(name, (i)->new PanelBlockItem(blockReg.getEntry(), i)).setParent(this);
    }

    @Override
    public Block getEntry() {
        return blockReg.getEntry();
    }

    @Override
    public PanelReg requiredFeatures(FeatureFlag[] requiredFeatures) {
        return BlockRegConfigurations.super.requiredFeatures(requiredFeatures);
    }

    public Item getItem() {
        return itemReg.getEntry();
    }
}
