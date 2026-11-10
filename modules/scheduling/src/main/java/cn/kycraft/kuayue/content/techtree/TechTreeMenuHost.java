package cn.kycraft.kuayue.content.techtree;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

/**
 * Opens the slot menu of the blueprint table.
 *
 * <p>The table deliberately has no screen: the board is painted into the world on top of the model and
 * already shows the tree, so opening a {@code ModularUIScreen} as well would draw it twice. The menu only
 * carries the slots, which {@link TableSlotProjection} paints on the table top.</p>
 */
public final class TechTreeMenuHost {

    private TechTreeMenuHost() {
    }

    /**
     * Opens the slot menu of a table; server side only, so both ends create the same menu.
     *
     * <p>The order matters: the menu has to be opened before the client paints anything, because the
     * client-side board is bound to it through the player's {@code containerMenu}.</p>
     */
    static boolean openTable(ServerPlayer player, BlockPos pos) {
        player.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("tech_tree.board.title");
            }

            @Override
            public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, Inventory inventory,
                                                                                  Player opener) {
                return new TechTreeTableMenu(id, inventory, pos);
            }
        }, buf -> buf.writeBlockPos(pos));
        return true;
    }
}
