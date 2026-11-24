package cn.kycraft.kuayue.train;

import io.micronaut.context.annotation.Context;
import jakarta.annotation.PostConstruct;
import lib.kasuga.registration.data_driven.TypeHandlerRegistry;
import lib.kasuga.registration.data_driven.pipeline.DataDrivenBuildHooks;

/**
 * Installs the train handler and compiles a catalog after all registration phases complete.
 */
@Context
public final class TrainDataDrivenModule {
    private static boolean installed;

    /**
     * Installs the train pipeline before any registry dispatch can invoke the content builder.
     */
    public static synchronized void install() {
        if (installed) {
            return;
        }
        TypeHandlerRegistry.register(new TrainTypeHandler());
        DataDrivenBuildHooks.registerFinalizer(
                TrainDataDrivenTypes.HOST_MOD_ID,
                (context, prepared) -> new TrainOverlayMerger().compile(prepared)
        );
        installed = true;
    }

    @PostConstruct
    public void init() {
        install();
    }
}
