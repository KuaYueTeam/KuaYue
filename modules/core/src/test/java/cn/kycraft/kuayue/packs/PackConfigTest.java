package cn.kycraft.kuayue.packs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PackConfigTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void missingFileUsesDefaultsWithoutCreatingIt() throws IOException {
        Path path = temporaryDirectory.resolve("kuayue/config.json");
        PackConfigStore.LoadResult result = PackConfigStore.load(path);

        assertFalse(result.hasBlockingErrors());
        assertTrue(result.document().config().enabled().isEmpty());
        assertFalse(Files.exists(path));

        PackConfig.ConfigCompletion completion = result.document().config().complete(
                Set.of("zeta-pack", "alpha-pack"));
        assertEquals(Map.of("kuayue-core", true), completion.config().enabled());
        assertEquals(List.of("kuayue-core", "alpha-pack", "zeta-pack"),
                completion.config().order());
        assertEquals(List.of("alpha-pack", "zeta-pack"), completion.config().inventory());
        assertFalse(completion.config().enabled().containsKey("alpha-pack"));
    }

    @Test
    void preservesExplicitSwitchesAndUnknownFieldsWhenSaving() throws IOException {
        Path path = temporaryDirectory.resolve("kuayue/config.json");
        Files.createDirectories(path.getParent());
        Files.writeString(path, """
                {
                  "other": {"keep": true},
                  "packs": {
                    "format_version": 1,
                    "enabled": {
                      "kuayue-core": false,
                      "zeta-pack": true
                    },
                    "order": ["zeta-pack", "zeta-pack", "unknown-pack"],
                    "inventory": ["zeta-pack"]
                  }
                }
                """);

        PackConfigStore.LoadResult loaded = PackConfigStore.load(path);
        assertFalse(loaded.hasBlockingErrors());
        PackConfig.ConfigCompletion completion =
                loaded.document().config().complete(Set.of("alpha-pack", "zeta-pack"));
        assertEquals(Boolean.FALSE, completion.config().enabled().get("kuayue-core"));
        assertEquals(Boolean.TRUE, completion.config().enabled().get("zeta-pack"));
        assertEquals(List.of("zeta-pack", "unknown-pack", "kuayue-core", "alpha-pack"),
                completion.config().order());

        PackConfigStore.write(path, loaded.document().withConfig(completion.config()));
        String written = Files.readString(path);
        assertTrue(written.contains("\"other\""));
        assertTrue(written.contains("\"keep\": true"));

        PackConfigStore.LoadResult reloaded = PackConfigStore.load(path);
        assertEquals(completion.config(), reloaded.document().config());
    }

    @Test
    void malformedExistingFileFailsClosedAndIsNotOverwritten() throws IOException {
        Path path = temporaryDirectory.resolve("kuayue/config.json");
        Files.createDirectories(path.getParent());
        String malformed = "{\"packs\":{\"format_version\":1,}}";
        Files.writeString(path, malformed);

        PackConfigStore.LoadResult loaded = PackConfigStore.load(path);
        assertTrue(loaded.hasBlockingErrors());
        assertEquals(malformed, Files.readString(path));
    }
}
