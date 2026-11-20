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
    @PostConstruct
    public void init() {
        TypeHandlerRegistry.register(new TrainTypeHandler());
        DataDrivenBuildHooks.registerFinalizer(
                TrainDataDrivenTypes.HOST_MOD_ID,
                (context, prepared) -> new TrainOverlayMerger().compile(prepared)
        );
    }
}
