package cn.kycraft.kuayue.content.techtree;

import net.minecraft.core.BlockPos;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Container menu of the blueprint table whose slots stay on the physical table top.
 *
 * <p>Deliberately a plain {@link AbstractContainerMenu} rather than a {@code ModularUIContainerMenu}:
 * the LDLib2 subclass calls {@code holder.createUI(player)} in its constructor and then dereferences the
 * result, so a block that hosts no screen at all cannot use it.</p>
 *
 * <p>Vanilla never draws these slots, but they are real {@link Slot}s, so clicking, shift-clicking and
 * pickup run through the vanilla menu pair, and {@link TableSlotProjection} paints them where the model's
 * table top is.</p>
 */
public class TechTreeTableMenu extends AbstractContainerMenu {

    /** Number of consumption slots per table. */
    static final int SLOTS = 9;

    /** Board-space pixels; {@link TableSlotProjection} maps them onto the table top. */
    static final int SLOT_X = 92;
    static final int SLOT_Y = 258;
    static final int SLOT_SIZE = 30;
    private static final int SLOT_GAP = 36;

    private final SimpleContainer items = new SimpleContainer(SLOTS);
    /** Table this menu belongs to; the client needs it to find the block entity to paint on. */
    private final BlockPos pos;

    public TechTreeTableMenu(int id, Inventory inventory, BlockPos pos) {
        super(TechTreeTableMenuType.get(), id);
        this.pos = pos;
        for (int i = 0; i < SLOTS; i++) {
            addSlot(new ProjectedSlot(items, i, SLOT_X + i * SLOT_GAP, SLOT_Y, SLOT_SIZE));
        }
    }

    SimpleContainer items() {
        return items;
    }

    public BlockPos pos() {
        return pos;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // Intentional: the slots are a receptacle for the tree, not a storage block.
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Nullable
    static TechTreeTableMenu of(Player player) {
        return player != null && player.containerMenu instanceof TechTreeTableMenu menu ? menu : null;
    }

    /** Every projected slot, in slot order. */
    static List<ProjectedSlot> projected(TechTreeTableMenu menu) {
        List<ProjectedSlot> slots = new ArrayList<>();
        if (menu == null) return slots;
        for (Slot slot : menu.slots) {
            if (slot instanceof ProjectedSlot projected) slots.add(projected);
        }
        return slots;
    }

    /** A real slot plus its projected rectangle in board pixels. */
    public static final class ProjectedSlot extends Slot {
        private final int boardX;
        private final int boardY;
        private final int boardSize;

        ProjectedSlot(SimpleContainer container, int index, int boardX, int boardY, int boardSize) {
            super(container, index, 0, 0);
            this.boardX = boardX;
            this.boardY = boardY;
            this.boardSize = boardSize;
        }

        public int boardX() {
            return boardX;
        }

        public int boardY() {
            return boardY;
        }

        public int boardSize() {
            return boardSize;
        }
    }
}
