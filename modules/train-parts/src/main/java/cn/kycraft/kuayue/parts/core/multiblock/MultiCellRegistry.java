package cn.kycraft.kuayue.parts.core.multiblock;

import cn.kycraft.kuayue.parts.KuaYueTrainPanelModule;
import io.micronaut.context.annotation.Context;
import lib.kasuga.registration.RegistryGroup;
import lib.kasuga.registration.minecraft.block.BlockReg;
import lib.kasuga.registration.minecraft.block.BlockRegConfigurations;
import lib.kasuga.registration.minecraft.block_entity.BlockEntityReg;

@Context
public class MultiCellRegistry {
    public static final RegistryGroup MULTI_CELL_REGISTRY = new RegistryGroup()
            .configure(BlockRegConfigurations.adopt(block -> block
                    .noCollission()
                    .noOcclusion()
                    .strength(1.5f, 3f)
                    .noLootTable()))
            .setParent(KuaYueTrainPanelModule.RUNTIME_REGISTRY_GROUP);

    public static final BlockReg<MultiCellCompanionBlock> MULTI_CELL_COMPANION =
            BlockReg.of("multi_cell_companion", MultiCellCompanionBlock::new)
                    .setParent(MULTI_CELL_REGISTRY);

    public static final BlockEntityReg<MultiCellCompanionBlockEntity> MULTI_CELL_COMPANION_ENTITY =
            BlockEntityReg.of("multi_cell_companion", MultiCellCompanionBlockEntity::new)
                    .validBlocks(MULTI_CELL_COMPANION::getEntry)
                    .setParent(MULTI_CELL_REGISTRY);
}
