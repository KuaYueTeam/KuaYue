package cn.kycraft.kuayue.content.techtree;

import lib.kasuga.registration.minecraft.block.BlockReg;
import lib.kasuga.registration.minecraft.block_entity.BlockEntityReg;
import cn.kycraft.kuayue.content.techtree.net.TechTreeRpc;
import net.minecraft.core.BlockPos;
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

    /** Backing store of each table's slots; the LDLib2 menu reads and writes these. */
    private static final java.util.Map<BlockPos, TableSlots> SLOTS = new java.util.concurrent.ConcurrentHashMap<>();

    private static boolean initialized;

    private BlueprintTableRegistry() {
    }

    /** Slot store of one table, created on first use. */
    static TableSlots slotsFor(BlockPos pos) {
        return SLOTS.computeIfAbsent(pos.immutable(), TableSlots::new);
    }

    /** Drops a table's slots when the block is removed. */
    static void dropSlots(BlockPos pos) {
        SLOTS.remove(pos);
    }

    public static synchronized void init() {
        if (initialized) return;
        initialized = true;
        TechTreeRpc.register(TechTreeSystem.REGISTRY);
    }
}
