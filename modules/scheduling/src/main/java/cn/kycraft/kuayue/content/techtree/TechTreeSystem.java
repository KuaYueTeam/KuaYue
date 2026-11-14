package cn.kycraft.kuayue.content.techtree;

import lib.kasuga.registration.Registry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tech tree system root.
 *
 * <p>Content is loaded from {@code data/<namespace>/tech_tree/*.json} (one file per group), the
 * player's unlock state lives in a per-player NBT attachment, and every client/server exchange goes
 * through {@link cn.kycraft.kuayue.content.techtree.net.TechTreeRpc} (KasugaLibNext RPC).</p>
 */
public final class TechTreeSystem {

    /** Own registry root: the scheduling module has no shared KuaYue registry object on its classpath. */
    public static final Registry REGISTRY = new Registry("kuayue");

    public static final Logger LOGGER = LoggerFactory.getLogger("KuaYue/TechTree");

    private TechTreeSystem() {
    }

    static void logInfo(String message, Object... args) {
        LOGGER.info(message, args);
    }

    static void logWarn(String message, Object... args) {
        LOGGER.warn(message, args);
    }

    static void logError(String message, Throwable error) {
        LOGGER.error(message, error);
    }
}
