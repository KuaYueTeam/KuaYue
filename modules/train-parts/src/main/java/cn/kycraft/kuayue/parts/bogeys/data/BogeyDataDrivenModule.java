package cn.kycraft.kuayue.parts.bogeys.data;

import io.micronaut.context.annotation.Context;
import jakarta.annotation.PostConstruct;
import lib.kasuga.registration.data_driven.TypeHandlerRegistry;
import lib.kasuga.registration.data_driven.property.JsonPropertyParser;
import net.minecraft.world.level.block.SoundType;

/**
 * Registers the stage-one bogey schemas and their KuaYue-specific property extension.
 */
@Context
public final class BogeyDataDrivenModule {

    /**
     * Installs bogey type handlers before Kasuga processes the mod JSON index.
     */
    @PostConstruct
    public void init() {
        JsonPropertyParser.getInstance().registerCompiler("netherite_block", (key, value) -> {
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()
                    || !value.getAsBoolean()) {
                return null;
            }
            return properties -> {
                properties.sound(SoundType.NETHERITE_BLOCK);
                return properties;
            };
        });
        TypeHandlerRegistry.register(new BogeySizeTypeHandler());
        TypeHandlerRegistry.register(new BogeyBlockTypeHandler());
        TypeHandlerRegistry.register(new BogeyStyleTypeHandler());
    }
}
