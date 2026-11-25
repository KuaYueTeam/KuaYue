package cn.kycraft.kuayue.client.creative;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CreativeFilterCatalogTest {
    @Test
    void sortsByExplicitOrderThenSubjectId() {
        List<CreativeFilterCatalog.Entry> entries = new ArrayList<>(List.of(
                entry("test:unset", Integer.MAX_VALUE),
                entry("test:maintenance", 101),
                entry("test:jy290", 100)
        ));

        CreativeFilterCatalog.sortEntries(entries);

        assertEquals(
                List.of("test:jy290", "test:maintenance", "test:unset"),
                entries.stream().map(entry -> entry.subjectId().toString()).toList()
        );
    }

    private static CreativeFilterCatalog.Entry entry(String subject, int order) {
        return new CreativeFilterCatalog.Entry(
                "test",
                ResourceLocation.parse(subject),
                ResourceLocation.parse("test:tab"),
                "test.name",
                "test.description",
                null,
                order,
                List.of(),
                Set.of()
        );
    }
}
