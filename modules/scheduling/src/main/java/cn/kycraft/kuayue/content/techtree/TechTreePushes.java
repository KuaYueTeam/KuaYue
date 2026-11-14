package cn.kycraft.kuayue.content.techtree;

import cn.kycraft.kuayue.content.techtree.net.TechTreeRpc;
import net.minecraft.server.level.ServerPlayer;

/** Pushes an unsolicited snapshot refresh (visibility changes that happen outside the board). */
final class TechTreePushes {

    private TechTreePushes() {
    }

    static void send(ServerPlayer player) {
        TechTreeRpc.pushSnapshot(player, TreeSnapshot.build(player));
    }
}
