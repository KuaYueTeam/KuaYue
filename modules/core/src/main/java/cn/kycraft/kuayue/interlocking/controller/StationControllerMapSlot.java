package cn.kycraft.kuayue.interlocking.controller;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class StationControllerMapSlot extends Slot {
    private final boolean input;

    public StationControllerMapSlot(Container container, int slot, int x, int y, boolean input) {
        super(container, slot, x, y);
        this.input = input;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return input && (stack.getItem() instanceof StationMap);
    }

}
