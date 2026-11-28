package cn.kycraft.kuayue.packs.shapes;

import com.google.gson.JsonObject;
import lib.kasuga.geometry.Box;
import lib.kasuga.geometry.Shape;
import lib.kasuga.registration.data_driven.handler.BlockDef;
import lib.kasuga.registration.data_driven.pipeline.DefinitionKey;
import lib.kasuga.registration.data_driven.pipeline.LocatedDefinition;
import lib.kasuga.registration.data_driven.shape.ShapeCatalog;
import lib.kasuga.registration.data_driven.source.LogicalPath;
import lib.kasuga.registration.data_driven.source.SourceDescriptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class KuaYueShapeDigestTest {
    @AfterEach
    void reset() {
        KuaYueShapeDigest.resetForTests();
    }

    @Test
    void compiledOutputAndTransitiveReferenceShapeChangesAlterTheDigest() {
        Shape target = Shape.of(Box.fromPixels(0, 0, 0, 8, 16, 16));
        Shape caller = target.union(Shape.of(Box.fromPixels(8, 0, 0, 16, 16, 16)));
        KuaYueShapeDigest.install(catalog(target, caller), List.of());
        String initial = KuaYueShapeDigest.current();

        Shape changedTarget = Shape.of(Box.fromPixels(0, 0, 0, 7, 16, 16));
        Shape changedCaller = changedTarget.union(
                Shape.of(Box.fromPixels(7, 0, 0, 16, 16, 16))
        );
        KuaYueShapeDigest.install(catalog(changedTarget, changedCaller), List.of());

        assertNotEquals(initial, KuaYueShapeDigest.current());
        assertEquals(64, KuaYueShapeDigest.current().length());
    }

    @Test
    void catalogEntryOrderDoesNotAffectTheDigest() {
        Shape a = Shape.of(Box.fromPixels(0, 0, 0, 8, 16, 16));
        Shape b = Shape.of(Box.fromPixels(8, 0, 0, 16, 16, 16));
        Map<String, ShapeCatalog.Entry> first = Map.of(
                "example:a", new ShapeCatalog.Entry("example:a", "x", a),
                "example:b", new ShapeCatalog.Entry("example:b", "x", b)
        );
        Map<String, ShapeCatalog.Entry> second = Map.of(
                "example:b", new ShapeCatalog.Entry("example:b", "x", b),
                "example:a", new ShapeCatalog.Entry("example:a", "x", a)
        );

        KuaYueShapeDigest.install(new ShapeCatalog(first), List.of());
        String one = KuaYueShapeDigest.current();
        KuaYueShapeDigest.install(new ShapeCatalog(second), List.of());

        assertEquals(one, KuaYueShapeDigest.current());
    }

    @Test
    void bindingsUseShapeBindingsWithoutAcceptingTheLegacyKey() {
        Shape target = Shape.of(Box.fromPixels(0, 0, 0, 8, 16, 16));
        Shape caller = target.union(Shape.of(Box.fromPixels(8, 0, 0, 16, 16, 16)));
        ShapeCatalog catalog = catalog(target, caller);

        KuaYueShapeDigest.install(catalog, List.of());
        String noBindings = KuaYueShapeDigest.current();
        KuaYueShapeDigest.install(
                catalog,
                List.of(block("shapes", "example:target"))
        );
        assertEquals(noBindings, KuaYueShapeDigest.current());

        KuaYueShapeDigest.install(
                catalog,
                List.of(block("shape_bindings", "example:target"))
        );
        String targetBinding = KuaYueShapeDigest.current();
        assertNotEquals(noBindings, targetBinding);

        KuaYueShapeDigest.install(
                catalog,
                List.of(block("shape_bindings", "example:caller"))
        );
        assertNotEquals(targetBinding, KuaYueShapeDigest.current());
    }

    private static ShapeCatalog catalog(Shape target, Shape caller) {
        return new ShapeCatalog(Map.of(
                "example:target", new ShapeCatalog.Entry("example:target", "box", target),
                "example:caller", new ShapeCatalog.Entry("example:caller", "box", caller)
        ));
    }

    private static LocatedDefinition<BlockDef> block(String bindingField, String shapeId) {
        JsonObject selection = new JsonObject();
        selection.addProperty("ref", shapeId);
        JsonObject binding = new JsonObject();
        binding.add("shape", selection);
        JsonObject params = new JsonObject();
        params.add(bindingField, binding);
        BlockDef definition = new BlockDef(
                "example:block",
                "example:factory",
                null,
                null,
                null,
                params
        );
        return new LocatedDefinition<>(
                new DefinitionKey("blocks", definition.id()),
                new KuaYueBlockTypeHandler(),
                definition,
                SourceDescriptor.of("test_source", "test_owner", Set.of("example")),
                0,
                LogicalPath.of("blocks.json"),
                "/blocks/0",
                0
        );
    }
}
