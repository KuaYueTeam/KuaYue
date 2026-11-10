package cn.kycraft.kuayue.content.techtree;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Blueprint table as a LDLib2 block UI: the world board is the view, {@link TechTreeTableMenu} the slots.
 *
 * <p>The table deliberately has no {@code ModularUIScreen}. The board is already painted into the world on
 * top of the model and shows the tree once, so {@link #openTable} opens the slot menu directly and the
 * client keeps rendering that board instead of a screen.</p>
 */
public interface TechTreeMenuHost extends BlockUIMenuType.BlockUI {

    @Override
    default ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        // No screen: the tree lives on the model, the menu only carries its slots.
        return null;
    }

    @Override
    default boolean stillValid(BlockUIMenuType.BlockUIHolder holder) {
        BlockState state = holder.player.level().getBlockState(holder.pos);
        return state.getBlock() == holder.blockState.getBlock()
                && holder.player.distanceToSqr(holder.pos.getX() + 0.5d,
                holder.pos.getY() + 0.5d, holder.pos.getZ() + 0.5d) <= 64.0d;
    }

    @Override
    default Component getUIDisplayName(BlockUIMenuType.BlockUIHolder holder) {
        return Component.translatable("tech_tree.board.title");
    }

    /**
     * Opens the slot menu of a table.
     *
     * <p>Server side only: the menu has to exist on both ends. The board is client-only and is opened by
     * the client half of {@link BlueprintTableBlock#useWithoutItem}, which runs on the same click.</p>
     */
    static boolean openTable(ServerPlayer player, BlockPos pos) {
        BlockUIMenuType.BlockUIHolder holder = TechTreeTableMenuType.holder(player, pos);
        player.openMenu(new net.minecraft.world.MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("tech_tree.board.title");
            }

            @Override
            public net.minecraft.world.inventory.AbstractContainerMenu createMenu(
                    int id, net.minecraft.world.entity.player.Inventory inventory,
                    net.minecraft.world.entity.player.Player opener) {
                return new TechTreeTableMenu(id, inventory, holder);
            }
        }, buf -> buf.writeBlockPos(pos));
        return true;
    }
}
