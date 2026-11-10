package cn.kycraft.kuayue.parts.core.resource;

import cn.kycraft.kuayue.KuaYue;
import com.mojang.logging.LogUtils;
import io.micronaut.context.annotation.Context;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lib.kasuga.core.resource.Resources;
import lib.kasuga.inject.class_loader.BeanOnlyIn;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/** Copies bundled Create schematics into the client's schematics directory. */
@Context
@BeanOnlyIn.Client
public final class KuaYueSchematicDistribution {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation BUNDLED_SCHEMATICS =
            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "schematic");
    private static final Path OUTPUT_DIRECTORY = Path.of("schematics");

    @Inject
    @Named("modEventBus")
    IEventBus modEventBus;

    @PostConstruct
    void init() {
        modEventBus.addListener(this::onClientSetup);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(KuaYueSchematicDistribution::distribute);
    }

    static void distribute() {
        int discovered = 0;
        int distributed = 0;
        int skipped = 0;
        int failed = 0;

        try {
            Map<String, Resource> resources = Resources.getResources(BUNDLED_SCHEMATICS, false);
            discovered = resources.size();
            Files.createDirectories(OUTPUT_DIRECTORY);

            for (Map.Entry<String, Resource> entry : resources.entrySet()) {
                Path target = OUTPUT_DIRECTORY.resolve(entry.getKey()).normalize();
                if (!target.startsWith(OUTPUT_DIRECTORY) || !target.getFileName().toString().endsWith(".nbt")) {
                    failed++;
                    LOGGER.warn("Ignoring invalid bundled schematic path '{}'", entry.getKey());
                    continue;
                }
                if (Files.exists(target)) {
                    skipped++;
                    continue;
                }
                try (InputStream input = entry.getValue().open()) {
                    Files.copy(input, target);
                    distributed++;
                } catch (IOException failure) {
                    failed++;
                    LOGGER.warn("Unable to distribute bundled schematic '{}'", entry.getKey(), failure);
                }
            }
        } catch (IOException failure) {
            failed++;
            LOGGER.error("Unable to discover bundled KuaYue schematics", failure);
        }

        LOGGER.info(
                "Schematic distribution finished. discovered: {}, distributed: {}, skipped: {}, failed: {}",
                discovered,
                distributed,
                skipped,
                failed
        );
    }
}
