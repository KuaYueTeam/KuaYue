package cn.kycraft.kuayue.packs.runtime;

import cn.kycraft.kuayue.packs.PackDiscovery;
import cn.kycraft.kuayue.packs.PackLimits;
import lib.kasuga.core.resource.lifecycle.LogicalSide;
import lib.kasuga.core.resource.lifecycle.ResourceEvent;
import lib.kasuga.core.resource.routing.ResourceRouting;
import lib.kasuga.core.resource.source.ResourceSnapshot;
import lib.kasuga.core.resource.view.ResourceCatalog;
import lib.kasuga.registration.data_driven.source.ContentSource;
import lib.kasuga.registration.data_driven.source.LogicalPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PackResourceSnapshotterTest {
    @TempDir
    Path game;

    @Test
    void splitsPackRootAndParcelRootAndFreezesOriginalBytes() throws Exception {
        Path packRoot = game.resolve("kuayue/packs/sample");
        write(packRoot.resolve("pack.json"), """
                {
                  "format_version": 1,
                  "id": "sample",
                  "version": "1.0.0",
                  "namespaces": ["example"],
                  "name_key": "pack.sample.name",
                  "dependencies": {
                    "mods": {
                      "minecraft": "1.21.1",
                      "neoforge": "21.1.203",
                      "kuayue": "2.0.0",
                      "kasuga_lib": "1.0.0",
                      "create": "6.0.6"
                    },
                    "packs": {}
                  },
                  "parcels": ["parcels/car/parcel.json"]
                }
                """);
        write(packRoot.resolve("parcels/car/parcel.json"), """
                {
                  "format_version": 1,
                  "id": "example:car",
                  "name_key": "parcel.example.car.name",
                  "indexes": ["data/example/kasuga_lib/data_driven/car.json"]
                }
                """);
        write(
                packRoot.resolve("assets/example/lang/en_us.json"),
                "{\"main\":\"main\"}"
        );
        write(
                packRoot.resolve("assets/example/blockstates/override_block.json"),
                "{\"variants\":{\"facing=north\":{\"model\":\"example:block/override_block\"}}}"
        );
        write(
                packRoot.resolve("assets/example/models/block/override_block.json"),
                "{\"parent\":\"minecraft:block/cube_all\"}"
        );
        write(
                packRoot.resolve("assets/example/models/item/override_block.json"),
                "{\"parent\":\"example:block/override_block\"}"
        );
        write(
                packRoot.resolve("parcels/car/assets/example/lang/en_us.json"),
                "{\"main\":\"parcel\"}"
        );
        write(
                packRoot.resolve("parcels/car/data/example/kasuga_lib/data_driven/car.json"),
                "{\"on_register\":[]}"
        );
        write(
                packRoot.resolve("parcels/car/data/example/kasuga_lib/data_driven/ignored.json"),
                "{\"on_register\":[]}"
        );

        var catalog = PackDiscovery.scan(
                game.resolve("kuayue/packs"),
                PackLimits.defaults()
        );
        assertEquals(1, catalog.packs().size());

        try (var captured = PackResourceSnapshotter.capture(
                packRoot,
                catalog.packs().getFirst(),
                PackLimits.defaults(),
                5
        )) {
            assertEquals(7, captured.nextRank());
            assertEquals(List.of("main", "parcel-car"), captured.roots().stream()
                    .map(PackResourceSnapshotter.CapturedRoot::rootId)
                    .toList());
            assertEquals(5, captured.roots().get(0).rank());
            assertEquals(6, captured.roots().get(1).rank());
            assertEquals("example:car", captured.roots().get(1).parcelId());

            PackResourceSnapshotter.CapturedRoot parcel = captured.roots().get(1);
            try (ResourceSnapshot main = captured.roots().getFirst().source().capture()) {
                assertEquals(
                        "{\"parent\":\"minecraft:block/cube_all\"}",
                        readText(main, "assets/example/models/block/override_block.json")
                );
                assertEquals(
                        "{\"parent\":\"example:block/override_block\"}",
                        readText(main, "assets/example/models/item/override_block.json")
                );
                assertTrue(main.files().stream().anyMatch(file ->
                        file.descriptor().logicalPath().equals(
                                "assets/example/lang/en_us.json")));
                ResourceRouting routing = new ResourceRouting(List.of(
                        new ResourceRouting.Binding(
                                "minecraft",
                                "sample",
                                "",
                                List.of(new ResourceRouting.Output(
                                        "minecraft",
                                        ResourceRouting.MINECRAFT_ENTRY_POINT,
                                        ResourceEvent.RESOURCE_RELOAD,
                                        LogicalSide.CLIENT,
                                        ""
                                ))
                        )
                ));
                List<String> targets = routing.route(
                        new ResourceCatalog("client", 1, List.of(main)),
                        ResourceEvent.RESOURCE_RELOAD,
                        LogicalSide.CLIENT
                ).stream().map(ResourceRouting.Route::targetPath).toList();
                assertTrue(targets.contains(
                        "assets/example/models/block/override_block.json"
                ));
                assertTrue(targets.contains(
                        "assets/example/models/item/override_block.json"
                ));
                assertTrue(targets.contains("assets/example/lang/en_us.json"));
            }
            try (ContentSource source = parcel.contentSource()) {
                assertEquals(List.of("data/example/kasuga_lib/data_driven/car.json"),
                        source.indexes().stream()
                                .map(file -> file.path().value())
                                .toList());
                assertTrue(source.exists(LogicalPath.of(
                        "data/example/kasuga_lib/data_driven/ignored.json")));
                assertFalse(source.indexes().stream().anyMatch(file ->
                        file.path().value().contains("ignored.json")));
            }

            write(
                    packRoot.resolve("parcels/car/assets/example/lang/en_us.json"),
                    "{\"main\":\"changed\"}"
            );
            try (ResourceSnapshot first = parcel.source().capture();
                 ResourceSnapshot second = parcel.source().capture()) {
                String path = "assets/example/lang/en_us.json";
                String firstText = readText(first, path);
                String secondText = readText(second, path);
                assertEquals("{\"main\":\"parcel\"}", firstText);
                assertEquals(firstText, secondText);
            }
        }
    }

    private static void write(Path path, String text) throws Exception {
        Files.createDirectories(path.getParent());
        Files.writeString(path, text, StandardCharsets.UTF_8);
    }

    private static String readText(ResourceSnapshot snapshot, String path) throws Exception {
        return new String(
                snapshot.files().stream()
                        .filter(file -> file.descriptor().logicalPath().equals(path))
                        .findFirst()
                        .orElseThrow()
                        .readBytes(),
                StandardCharsets.UTF_8
        );
    }
}
