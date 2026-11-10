package cn.kycraft.kuayue.content.techtree;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerMenu;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Container menu of the blueprint table whose slots stay on the physical table top.
 *
 * <p>The blueprint table already paints its board into the world on top of the model, so the LDLib2
 * menu-backed screen would show the same tree twice. This menu has no widgets: vanilla never draws the
 * slots, but they are real {@link Slot}s, so clicking, shift-clicking and pickup run through the vanilla
 * menu pair, and {@link TableSlotProjection} paints them where the model's table top is.</p>
 */
public class TechTreeTableMenu extends ModularUIContainerMenu {

    /** Number of consumption slots per table. */
    static final int SLOTS = 9;

    /** Board-space pixels; {@link TableSlotProjection} maps them onto the table top. */
    static final int SLOT_X = 92;
    static final int SLOT_Y = 258;
    static final int SLOT_SIZE = 30;
    private static final int SLOT_GAP = 36;

    private final SimpleContainer items = new SimpleContainer(SLOTS);

    /**
     * Used by the registered menu type.
     *
     * <p>{@link ModularUIContainerMenu} is parameterised on itself rather than on the concrete menu, so the
     * type is fetched as the base class; the unchecked cast is the natural erasure of that mismatch.</p>
     */
    @SuppressWarnings("unchecked")
    public TechTreeTableMenu(int id, Inventory inventory, BlockUIMenuType.BlockUIHolder holder) {
        super((MenuType<ModularUIContainerMenu>) (MenuType<?>) TechTreeTableMenuType.get(), id, inventory, holder);
        for (int i = 0; i < SLOTS; i++) {
            addSlot(new ProjectedSlot(items, i, SLOT_X + i * SLOT_GAP, SLOT_Y, SLOT_SIZE));
        }
    }

    SimpleContainer items() {
        return items;
    }

    @Override
    public ModularUI getModularUI() {
        return null;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // Intentional: the slots are a receptacle for the tree, not a storage block.
        return ItemStack.EMPTY;
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
