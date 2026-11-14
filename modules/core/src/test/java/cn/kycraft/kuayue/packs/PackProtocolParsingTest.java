package cn.kycraft.kuayue.packs;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PackProtocolParsingTest {
    @Test
    void parsesTheV1ManifestAndParcel() {
        PackManifest manifest = PackManifest.parse(validPackJson());

        assertEquals(1, manifest.formatVersion());
        assertEquals("example-railway", manifest.id());
        assertEquals(SemanticVersion.parse("1.0.0"), manifest.version());
        assertEquals(Set.of("example_railway", "kuayue"), Set.copyOf(manifest.namespaces()));
        assertEquals(Set.of("minecraft", "neoforge", "kuayue", "kasuga_lib", "create"),
                manifest.dependencies().mods().keySet());
        assertEquals(1, manifest.parcels().size());

        ParcelManifest parcel = ParcelManifest.parse(validParcelJson(), manifest.namespaces());
        assertEquals("example_railway:m1", parcel.id());
        assertEquals(1, parcel.indexes().size());
        assertTrue(parcel.indexes().get(0).startsWith("data/example_railway/"));
    }

    @Test
    void rejectsNonStrictJson() {
        assertThrows(IllegalArgumentException.class,
                () -> StrictJson.validate("{\"a\":1,\"a\":2}"));
        assertThrows(IllegalArgumentException.class,
                () -> StrictJson.validate("{\"a\":1, // comment\n}"));
        assertThrows(IllegalArgumentException.class,
                () -> StrictJson.validate("{\"a\":1,}"));
    }

    @Test
    void rejectsUnknownAndInvalidManifestFields() {
        String unknown = validPackJson().replace(
                "\"format_version\": 1,",
                "\"format_version\": 1, \"unexpected\": true,");
        assertThrows(IllegalArgumentException.class, () -> PackManifest.parse(unknown));

        String unsupported = validPackJson().replace("\"format_version\": 1", "\"format_version\": 2");
        PackParseException unsupportedError =
                assertThrows(PackParseException.class, () -> PackManifest.parse(unsupported));
        assertEquals("UNSUPPORTED_FORMAT", unsupportedError.code());

        String namespace = "Example";
        String invalidNamespace = validPackJson().replace(
                "\"namespaces\": [\"example_railway\", \"kuayue\"]",
                "\"namespaces\": [\"" + namespace + "\", \"kuayue\"]");
        PackParseException namespaceError =
                assertThrows(PackParseException.class, () -> PackManifest.parse(invalidNamespace));
        assertEquals("INVALID_NAMESPACE", namespaceError.code());
    }

    @Test
    void validatesDependencyRangesAndPackIdentity() {
        assertTrue(VersionRange.parse("*").matches(SemanticVersion.parse("999.0.0")));
        assertTrue(VersionRange.parse("[1.2.0,2.0.0)").matches(SemanticVersion.parse("1.9.9")));
        assertFalse(VersionRange.parse("[1.2.0,2.0.0)").matches(SemanticVersion.parse("2.0.0")));
        assertThrows(IllegalArgumentException.class, () -> VersionRange.parse("^1.2.0"));
        assertThrows(IllegalArgumentException.class, () -> VersionRange.parse("[2.0.0,1.0.0)"));
        assertThrows(IllegalArgumentException.class, () -> SemanticVersion.parse("1.0"));
        assertThrows(IllegalArgumentException.class, () -> SemanticVersion.parse("01.0.0"));

        String reserved = validPackJson().replace(
                "\"id\": \"example-railway\"",
                "\"id\": \"kuayue-core\"");
        PackParseException error =
                assertThrows(PackParseException.class, () -> PackManifest.parse(reserved));
        assertEquals("INVALID_MANIFEST", error.code());
    }

    @Test
    void evaluatesOnlyDirectHistoricalVersionCompatibility() {
        SaveCompatibility exact = new SaveCompatibility(
                java.util.List.of(SemanticVersion.parse("0.1.0")),
                null);
        assertTrue(exact.allowsUpgradeFrom(
                SemanticVersion.parse("0.1.0"), SemanticVersion.parse("0.2.0")));
        assertFalse(exact.allowsUpgradeFrom(
                SemanticVersion.parse("0.1.5"), SemanticVersion.parse("0.2.0")));

        SaveCompatibility minimum = new SaveCompatibility(
                null,
                SemanticVersion.parse("0.2.0"));
        assertTrue(minimum.allowsUpgradeFrom(
                SemanticVersion.parse("0.2.0"), SemanticVersion.parse("0.3.0")));
        assertFalse(minimum.allowsUpgradeFrom(
                SemanticVersion.parse("0.1.0"), SemanticVersion.parse("0.3.0")));
        assertTrue(minimum.allowsUpgradeFrom(
                SemanticVersion.parse("0.3.0"), SemanticVersion.parse("0.3.0")));
        assertFalse(minimum.allowsUpgradeFrom(
                SemanticVersion.parse("0.4.0"), SemanticVersion.parse("0.3.0")));
    }

    @Test
    void rejectsInvalidSaveCompatibilityShapes() {
        String mixed = validPackJson().replace(
                "\"save_compatibility\": {\"minimum_version\": \"0.2.0\"}",
                "\"save_compatibility\": {\"minimum_version\": \"0.2.0\", "
                        + "\"from_versions\": [\"0.1.0\"]}");
        assertThrows(PackParseException.class, () -> PackManifest.parse(mixed));

        String tooNew = validPackJson().replace(
                "\"save_compatibility\": {\"minimum_version\": \"0.2.0\"}",
                "\"save_compatibility\": {\"minimum_version\": \"1.0.0\"}");
        assertThrows(PackParseException.class, () -> PackManifest.parse(tooNew));
    }

    private static String validPackJson() {
        return """
                {
                  "format_version": 1,
                  "id": "example-railway",
                  "version": "1.0.0",
                  "namespaces": ["example_railway", "kuayue"],
                  "name_key": "pack.example_railway.name",
                  "description_key": "pack.example_railway.description",
                  "dependencies": {
                    "mods": {
                      "minecraft": "1.21.1",
                      "neoforge": "[21.1.203,22.0.0)",
                      "kuayue": "[2.0.0,3.0.0)",
                      "kasuga_lib": "[1.0.0,2.0.0)",
                      "create": "[6.0.6,7.0.0)"
                    },
                    "packs": {}
                  },
                  "parcels": ["parcels/m1/parcel.json"],
                  "save_compatibility": {"minimum_version": "0.2.0"}
                }
                """;
    }

    private static String validParcelJson() {
        return """
                {
                  "format_version": 1,
                  "id": "example_railway:m1",
                  "name_key": "parcel.example_railway.m1.name",
                  "description_key": "parcel.example_railway.m1.description",
                  "icon": "example_railway:textures/pack/m1.png",
                  "indexes": [
                    "data/example_railway/kasuga_lib/data_driven/m1.json"
                  ]
                }
                """;
    }
}
