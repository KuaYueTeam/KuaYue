package cn.kycraft.kuayue.parts.core.panel.company;

import cn.kycraft.kuayue.parts.KuaYueTrainPanelModule;
import lib.kasuga.registration.RegistryGroup;
import lib.kasuga.registration.minecraft.block.BlockReg;
import lib.kasuga.registration.minecraft.block.BlockRegConfigurations;
import lib.kasuga.registration.minecraft.block_entity.BlockEntityReg;

public class CompanyRegistry {
    public static final RegistryGroup COMPANY_REGISTRY = new RegistryGroup()
            .configure(BlockRegConfigurations.adopt(block -> block
                    .noCollission()
                    .strength(1.5f, 3f)
                    .requiresCorrectToolForDrops()))
            .setParent(KuaYueTrainPanelModule.REGISTRY_GROUP);

    public static final BlockReg<CompanyTrainPanel> COMPANY_TRAIN_PANEL =
            BlockReg.of("company_panel_block", CompanyTrainPanel::new)
                    .setParent(COMPANY_REGISTRY);

    public static final BlockReg<CompanyTrainDoor> COMPANY_TRAIN_DOOR =
            BlockReg.of("company_panel_door", CompanyTrainDoor::new)
                    .setParent(COMPANY_REGISTRY);

    public static final BlockReg<CompanyTrainSlab> COMPANY_FLOOR =
            BlockReg.of("company_panel_floor", p -> new CompanyTrainSlab(p, false))
                    .setParent(COMPANY_REGISTRY);

    public static final BlockReg<CompanyTrainSlab> COMPANY_CARPORT =
            BlockReg.of("company_panel_carport", p -> new CompanyTrainSlab(p, true))
                    .setParent(COMPANY_REGISTRY);

    public static final BlockEntityReg<CompanyTrainBlockEntity> COMPANY_TRAIN_BLOCK_ENTITY =
            BlockEntityReg.of("company_panel", CompanyTrainBlockEntity::new)
                    .setParent(COMPANY_REGISTRY);
}
