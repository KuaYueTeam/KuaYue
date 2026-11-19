package cn.kycraft.kuayue.content.techtree;

import net.minecraft.server.level.ServerPlayer;

import java.util.Set;
import java.util.WeakHashMap;

/**
 * Server-side note of which players currently have a blueprint board open.
 *
 * <p>Nothing in the unlock path depends on it: the tree is stateless per player, and every request is
 * judged against the {@link UnlockState} attachment. It exists so the open/close RPCs have a real
 * receiver, and so a later feature (board-scoped broadcasts, "someone is reading the plans" state, or
 * expiring a stale board after a timeout) has one place to hang on.</p>
 */
public final class TechTreeServerSessions {

    private static final Set<ServerPlayer> OPEN =
            java.util.Collections.newSetFromMap(new WeakHashMap<>());

    private TechTreeServerSessions() {
    }

    public static void open(ServerPlayer player) {
        if (player != null) OPEN.add(player);
    }

    public static void close(ServerPlayer player) {
        if (player != null) OPEN.remove(player);
    }

    public static boolean isOpen(ServerPlayer player) {
        return OPEN.contains(player);
    }
}
