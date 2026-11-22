package cn.kycraft.kuayue.content.tech_tree;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Marker interface: {@link BlueprintTableBlock} is a block the tech tree board can be mounted on.
 *
 * <p>This used to be an LDLib2 {@code BlockUIMenuType.BlockUI} host, which is what the table's right
 * click opened: a container menu whose screen covered the world. The board is now painted on the table
 * top itself by {@link TechTreeTableSurface} through KasugaLibNext's world-UI backend, so there is no
 * menu and no screen left to build here. The interface stays because the block, the persistence of the
 * selected group and the display name are still table concerns.</p>
 */
public interface TechTreeMenuHost {

    /** Board size in GUI pixels, matching the blueprint sheet. */
    int BOARD_W = 300;
    int BOARD_H = 150;

    /** Display name of the board, used by the accessibility title and by the screen host. */
    static Component displayName() {
        return Component.translatable("tech_tree.board.title");
    }

    /**
     * Books the server side of "the board is open".
     *
     * <p>The tree itself needs no container: the client builds it from the snapshot it already has. The
     * server is still told, because the open request is what refreshes that snapshot (and what tells the
     * server the player is looking at the board, if a later feature wants it).</p>
     */
    static boolean openTable(ServerPlayer player, BlockPos pos) {
        if (player == null || pos == null) return false;
        TechTreePushes.send(player);
        return true;
    }
}
