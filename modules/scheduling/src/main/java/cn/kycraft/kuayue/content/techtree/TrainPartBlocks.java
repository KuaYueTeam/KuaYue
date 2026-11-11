package cn.kycraft.kuayue.content.techtree;

import cn.kycraft.kuayue.parts.KuaYueTrainPanelModule;
import cn.kycraft.kuayue.parts.locomotives.ss.ISS8Panel;

/**
 * Touches the train-part registration holders before a blueprint is decoded.
 *
 * <p>Part blocks are declared as static fields on Micronaut {@code @Context} beans, and a blueprint
 * decoder rejects the whole file on a single unknown palette id. Reading one of those static fields is
 * what runs the holder's initialiser and thereby registers its {@code BlockReg} entries.</p>
 *
 * <p>This is a plain static reference rather than {@code Class.forName}: in the dev client every module is
 * loaded through its own transformer class loader, so lookup by name from this class fails even though the
 * jar is on the classpath. A compiled reference is resolved by the module layer instead, which does see the
 * sibling module.</p>
 */
final class TrainPartBlocks {

    private static volatile boolean loaded;
    private static volatile boolean available;

    private TrainPartBlocks() {
    }

    /** True once {@link #ensureLoaded()} ran and the part holders were reachable. */
    static boolean available() {
        return available;
    }

    /** Idempotent; safe to call from the render thread before decoding. */
    static void ensureLoaded() {
        if (loaded) return;
        synchronized (TrainPartBlocks.class) {
            if (loaded) return;
            try {
                // Reading the group field initialises the holder, which registers every panel in it.
                Object group = KuaYueTrainPanelModule.REGISTRY_GROUP;
                Object ss8 = ISS8Panel.GROUP_SS8;
                available = group != null && ss8 != null;
            } catch (Throwable error) {
                TechTreeSystem.LOGGER.warn("Train part holders could not be initialised", error);
                available = false;
            }
            loaded = true;
            TechTreeSystem.LOGGER.info("Train part blocks for blueprint previews: {}",
                    available ? "available" : "missing");
        }
    }
}
