package cn.kycraft.kuayue.content.techtree;

import net.minecraft.core.BlockPos;
import net.minecraft.world.SimpleContainer;

/**
 * Item store of one blueprint table.
 *
 * <p>The table's slots are real {@link com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot}s inside the
 * LDLib2 menu UI; this container is only the backing store they read and write.</p>
 */
final class TableSlots extends SimpleContainer {

    /** Number of consumption slots per table. */
    static final int COUNT = 9;

    private final BlockPos pos;

    TableSlots(BlockPos pos) {
        super(COUNT);
        this.pos = pos;
    }

    BlockPos pos() {
        return pos;
    }
}
