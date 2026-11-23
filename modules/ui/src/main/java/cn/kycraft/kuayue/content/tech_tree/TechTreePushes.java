package cn.kycraft.kuayue.content.tech_tree;

import cn.kycraft.kuayue.content.tech_tree.net.TechTreeRpc;
import net.minecraft.server.level.ServerPlayer;

/** Pushes an unsolicited snapshot refresh (visibility changes that happen outside the board). */
final class TechTreePushes {

    private TechTreePushes() {
    }

    static void send(ServerPlayer player) {
        TechTreeRpc.pushSnapshot(player, TreeSnapshot.build(player));
    }
}
