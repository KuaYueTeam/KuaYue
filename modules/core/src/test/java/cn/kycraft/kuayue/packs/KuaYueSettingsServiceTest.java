package cn.kycraft.kuayue.packs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KuaYueSettingsServiceTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void requiredConfigEntriesAreIgnoredAndRemovedWhenSaving() throws Exception {
        Path configPath = temporaryDirectory.resolve("kuayue/config.json");
        Files.createDirectories(configPath.getParent());
        Files.writeString(configPath, """
                {
                  "packs": {
                    "format_version": 1,
                    "enabled": {
                      "required-pack": false,
                      "optional-pack": true
                    },
                    "order": ["required-pack", "optional-pack"],
                    "inventory": []
                  }
                }
                """);
        Map<String, PackManifest> manifests = Map.of(
                "required-pack", manifest("required-pack", true),
                "optional-pack", manifest("optional-pack", false)
        );
        KuaYueSettingsService service =
                new KuaYueSettingsService(temporaryDirectory, PackLimits.defaults());

        KuaYueSettingsService.LoadResult loaded = service.begin(manifests);

        assertTrue(loaded.successful());
        assertEquals(
                List.of("required-pack", "optional-pack"),
                loaded.session().draft().selectedIds()
        );
        assertEquals(
                List.of("optional-pack", "required-pack"),
                loaded.session().draft().selectedIdsByPriority()
        );

        KuaYueSettingsService.SaveResult saved = service.save(
                loaded.session(),
                loaded.session().draft()
        );

        assertTrue(saved.successful());
        PackConfig persisted = PackConfigStore.load(configPath).document().config();
        assertEquals(Map.of("optional-pack", true), persisted.enabled());
        assertEquals(List.of("optional-pack"), persisted.order());
    }

    private static PackManifest manifest(String id, boolean required) {
        return PackManifest.parse("""
                {
                  "format_version": 1,
                  "id": "%s",
                  "version": "1.0.0",
                  "required": %s,
                  "namespaces": ["%s"],
                  "name_key": "pack.%s.name",
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
                  "parcels": ["parcels/main/parcel.json"]
                }
                """.formatted(id, required, id.replace('-', '_'), id));
    }
}
