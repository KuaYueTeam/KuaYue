package cn.kycraft.kuayue.content.techtree;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Lets the blueprint table host a LDLib2 container UI.
 *
 * <p>This is what makes the consumption slots interactive: {@link BlockUIMenuType.BlockUI} routes the
 * screen through {@code ModularUIContainerMenu}, so every {@code ItemSlot} is backed by a real
 * {@code Slot} and the player's inventory is shown underneath like a chest.</p>
 *
 * <p>Note the split between the two views: the world-space board drawn on the table top is a plain
 * {@code Renderable} and is intentionally read-only (no slots). Dragging items only happens in this
 * menu-backed screen.</p>
 */
public interface TechTreeMenuHost extends BlockUIMenuType.BlockUI {

    @Override
    default ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        return TechTreeScreen.createModularUI(holder.player, holder.pos);
    }

    @Override
    default boolean stillValid(BlockUIMenuType.BlockUIHolder holder) {
        // Vanilla container reach check: the table must still be the block at that position.
        BlockState state = holder.player.level().getBlockState(holder.pos);
        return state.getBlock() == holder.blockState.getBlock()
                && holder.player.distanceToSqr(holder.pos.getX() + 0.5d,
                holder.pos.getY() + 0.5d, holder.pos.getZ() + 0.5d) <= 64.0d;
    }

    @Override
    default Component getUIDisplayName(BlockUIMenuType.BlockUIHolder holder) {
        return Component.translatable("tech_tree.board.title");
    }

    /** Opens the container-backed screen for a table; server side, so the menu is created on both ends. */
    static boolean openTable(net.minecraft.server.level.ServerPlayer player, BlockPos pos) {
        return BlockUIMenuType.openUI(player, pos);
    }
}
