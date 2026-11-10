package cn.kycraft.kuayue.content.techtree;

import cn.kycraft.kuayue.KuaYue;
import io.micronaut.context.ApplicationContext;

/**
 * Resolves the train-part beans before a blueprint is decoded.
 *
 * <p>Part blocks are declared as static fields on Micronaut {@code @Context} beans, and a blueprint
 * decoder rejects the whole file on a single unknown palette id. The beans must therefore exist before
 * the palette is resolved.</p>
 *
 * <p>Reflection through this class's own loader does not work: every mod is loaded through a transformer
 * class loader, so {@code cn.kycraft.kuayue.parts.*} is invisible from here even when the jar is on the
 * classpath. The mod's Micronaut context does see it, and asking the context for the bean is also what
 * instantiates it.</p>
 *
 * <p>{@code modules:scheduling} does not compile against {@code modules:train-parts}, so the bean types
 * are named as strings; a missing module is reported and skipped.</p>
 */
final class TrainPartBlocks {

    /**
     * Beans whose initialisation registers the blocks a shipped blueprint can reference.
     *
     * <p>Adding a blueprint that uses another locomotive family means adding its panel bean here.</p>
     */
    private static final String[] PANEL_BEANS = {
            "cn.kycraft.kuayue.parts.KuaYueTrainPanelModule",
            "cn.kycraft.kuayue.parts.locomotives.ss.ISS8Panel",
    };

    private static volatile boolean loaded;
    private static volatile boolean available;

    private TrainPartBlocks() {
    }

    /** True once {@link #ensureLoaded()} ran and at least one part bean was resolved. */
    static boolean available() {
        return available;
    }

    /** Idempotent; safe to call from the render thread before decoding. */
    static void ensureLoaded() {
        if (loaded) return;
        synchronized (TrainPartBlocks.class) {
            if (loaded) return;
            ApplicationContext context = KuaYue.getContext();
            boolean found = false;
            for (String name : PANEL_BEANS) {
                try {
                    Class<?> type = Class.forName(name, false, context.getClass().getClassLoader());
                    Object bean = context.getBean(type);
                    if (bean != null) found = true;
                } catch (ClassNotFoundException | io.micronaut.context.exceptions.NoSuchBeanException missing) {
                    TechTreeSystem.LOGGER.debug("Train part bean {} is not installed", name);
                } catch (Throwable error) {
                    TechTreeSystem.LOGGER.warn("Train part bean {} failed to initialise", name, error);
                    found = true;
                }
            }
            available = found;
            loaded = true;
            TechTreeSystem.LOGGER.info("Train part blocks for blueprint previews: {}",
                    available ? "available" : "missing");
        }
    }
}
