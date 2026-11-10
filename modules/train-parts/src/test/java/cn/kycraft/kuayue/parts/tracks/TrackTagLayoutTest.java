package cn.kycraft.kuayue.parts.tracks;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrackTagLayoutTest {

    private static final Path TRACK_DATA = Path.of("resources/tracks/data");
    private static final List<String> TRACKS = List.of(
            "kuayue:standard_track",
            "kuayue:ballastless_track",
            "kuayue:tieless_track",
            "kuayue:meter_track",
            "kuayue:guard_track"
    );

    @Test
    void trackTagsUseThe121RegistryDirectories() throws IOException {
        assertTrue(Files.isRegularFile(
                TRACK_DATA.resolve("create/tags/block/tracks.json")
        ));
        assertTrue(Files.isRegularFile(
                TRACK_DATA.resolve("create/tags/block/girdable_tracks.json")
        ));
        assertTrue(Files.isRegularFile(
                TRACK_DATA.resolve("create/tags/item/tracks.json")
        ));
        assertTrue(Files.isRegularFile(
                TRACK_DATA.resolve("minecraft/tags/block/mineable/pickaxe.json")
        ));
        assertTrue(Files.isRegularFile(
                TRACK_DATA.resolve("c/tags/block/relocation_not_supported.json")
        ));

        assertFalse(Files.exists(
                TRACK_DATA.resolve("create/tags/blocks/tracks.json")
        ));
        assertFalse(Files.exists(
                TRACK_DATA.resolve("create/tags/blocks/girdable_tracks.json")
        ));
        assertFalse(Files.exists(
                TRACK_DATA.resolve("minecraft/tags/blocks/mineable/pickaxe.json")
        ));
        assertFalse(Files.exists(
                TRACK_DATA.resolve("forge/tags/blocks/relocation_not_supported.json")
        ));

        String blockTag = Files.readString(
                TRACK_DATA.resolve("create/tags/block/tracks.json")
        );
        String itemTag = Files.readString(
                TRACK_DATA.resolve("create/tags/item/tracks.json")
        );
        for (String track : TRACKS) {
            assertTrue(blockTag.contains("\"" + track + "\""));
            assertTrue(itemTag.contains("\"" + track + "\""));
        }
    }
}
