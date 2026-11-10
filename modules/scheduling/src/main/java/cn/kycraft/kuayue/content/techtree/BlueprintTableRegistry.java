package cn.kycraft.kuayue.content.techtree;

import lib.kasuga.registration.minecraft.block.BlockReg;
import lib.kasuga.registration.minecraft.block_entity.BlockEntityReg;
import cn.kycraft.kuayue.content.techtree.net.TechTreeRpc;
import net.minecraft.world.level.block.SoundType;

/**
 * Registration of the blueprint table.
 *
 * <p>Registered eagerly in the mod's main registry group: the table is the entry point to the tech
 * tree, so it must not depend on a datapack-driven factory pass.</p>
 */
public final class BlueprintTableRegistry {

    public static final BlockReg<BlueprintTableBlock> BLOCK =
            BlockReg.of("blueprint_table", properties -> new BlueprintTableBlock(properties
                            .strength(2.5f, 6.0f)
                            .sound(SoundType.WOOD)
                            .noOcclusion()))
                    .withBlockItem("blueprint_table", () -> (block, props) -> new net.minecraft.world.item.BlockItem(block, props))
                    .setParent(TechTreeSystem.REGISTRY);

    public static final BlockEntityReg<BlueprintTableBlockEntity> BLOCK_ENTITY =
            BlockEntityReg.of("blueprint_table", BlueprintTableBlockEntity::new)
                    .validBlocks(BLOCK::getEntry)
                    .setParent(TechTreeSystem.REGISTRY);

    private static boolean initialized;

    private BlueprintTableRegistry() {
    }

    public static synchronized void init() {
        if (initialized) return;
        initialized = true;
        TechTreeRpc.register(TechTreeSystem.REGISTRY);
        // Touching the Reg makes sure the menu type is part of the group Kasuga walks on REGISTER_EVENT;
        // it must not be created on first right-click, when Registries.MENU is already frozen.
        TechTreeTableMenuType.REG.getClass();
    }
}
