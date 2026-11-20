package cn.kycraft.kuayue.train;

import com.google.gson.JsonParser;
import lib.kasuga.registration.data_driven.pipeline.BuildDiagnostic;
import lib.kasuga.registration.data_driven.pipeline.DefinitionKey;
import lib.kasuga.registration.data_driven.pipeline.HandlerCapability;
import lib.kasuga.registration.data_driven.pipeline.LocatedDefinition;
import lib.kasuga.registration.data_driven.pipeline.RegistryClaim;
import lib.kasuga.registration.data_driven.source.LogicalPath;
import lib.kasuga.registration.data_driven.source.SourceDescriptor;
import lib.kasuga.registration.data_driven.validation.ValidationContext;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrainTypeHandlerTest {
    private final TrainTypeHandler handler = new TrainTypeHandler();
    private final TrainPatchParser parser = new TrainPatchParser();

    @Test
    void exposesTheOverlayHandlerContract() {
        assertEquals(TrainDataDrivenTypes.TRAINS, handler.getTypeName());
        assertEquals(60, handler.getPhase());
        assertEquals(
                Set.of(
                        HandlerCapability.STRICT_PREFLIGHT,
                        HandlerCapability.NON_REGISTRATION,
                        HandlerCapability.OVERLAY
                ),
                handler.capabilities()
        );
        TrainPatch patch = parser.parse(JsonParser.parseString("""
                {"id":"kuayue:ss3"}
                """).getAsJsonObject());
        assertEquals("kuayue:ss3", handler.resolveIdentity("kuayue", patch));
    }

    @Test
    void validatesOnlyDeclaredHandlerReferenceLists() {
        LocatedDefinition<TrainPatch> definition = located(
                "pack_owner",
                10,
                1,
                """
                        {
                          "id":"kuayue:ss3",
                          "category":"locomotive",
                          "name_key":"train.kuayue.ss3.name",
                          "description_key":"train.kuayue.ss3.description",
                          "members":{
                            "blocks":["kuayue:ss3_body"],
                            "items":["kuayue:ss3_item"],
                            "item_tags":["kuayue:not_a_register_type"],
                            "bogey_sizes":["kuayue:ss3_size"],
                            "bogey_motions":["kuayue:ss3_motion"],
                            "bogey_models":["kuayue:ss3_model"],
                            "bogey_blocks":["kuayue:ss3_bogey"],
                            "bogey_styles":["kuayue:ss3_style"],
                            "bogey_style_variants":["kuayue:ss3_variant"]
                          }
                        }
                        """
        );
        RecordingContext context = new RecordingContext(definition, Set.of());

        handler.validate(definition.definition(), context);

        assertTrue(context.errors.isEmpty(), context.errors.toString());
        assertEquals(
                Set.of(
                        "blocks:kuayue:ss3_body",
                        "items:kuayue:ss3_item",
                        "bogey_sizes:kuayue:ss3_size",
                        "bogey_motions:kuayue:ss3_motion",
                        "bogey_models:kuayue:ss3_model",
                        "bogey_blocks:kuayue:ss3_bogey",
                        "bogey_styles:kuayue:ss3_style",
                        "bogey_style_variants:kuayue:ss3_variant"
                ),
                Set.copyOf(context.required)
        );
    }

    @Test
    void rejectsTheSameOwnerDeclaringOneTrainTwice() {
        LocatedDefinition<TrainPatch> first = located(
                "same_owner",
                10,
                1,
                minimalPatch("kuayue:ss3")
        );
        LocatedDefinition<TrainPatch> current = located(
                "same_owner",
                20,
                2,
                minimalPatch("kuayue:ss3")
        );
        RecordingContext context = new RecordingContext(
                current,
                Set.of(),
                List.of(first, current),
                List.of(first, current)
        );

        handler.validate(current.definition(), context);

        assertTrue(context.errors.stream()
                .anyMatch(error -> error.code == BuildDiagnostic.Code.CONTENT_INVALID));
    }

    @Test
    void requiresLowerPriorityContributorToBeDirectDependency() {
        LocatedDefinition<TrainPatch> base = located(
                "base_pack",
                10,
                1,
                completePatch("kuayue:ss3")
        );
        LocatedDefinition<TrainPatch> current = located(
                "override_pack",
                20,
                2,
                minimalPatch("kuayue:ss3")
        );

        RecordingContext missing = new RecordingContext(
                current,
                Set.of(),
                List.of(current),
                List.of(base, current)
        );
        handler.validate(current.definition(), missing);
        assertTrue(missing.errors.stream().anyMatch(error ->
                error.code == BuildDiagnostic.Code.REFERENCE_NOT_DECLARED));

        RecordingContext declared = new RecordingContext(
                current,
                Set.of("base_pack"),
                List.of(base, current),
                List.of(base, current)
        );
        handler.validate(current.definition(), declared);
        assertTrue(declared.errors.isEmpty(), declared.errors.toString());
    }

    private LocatedDefinition<TrainPatch> located(
            String owner,
            int sourceOrder,
            int declarationOrder,
            String json
    ) {
        TrainPatch patch = parser.parse(JsonParser.parseString(json).getAsJsonObject());
        SourceDescriptor source = SourceDescriptor.of(
                owner + "_source",
                owner,
                Set.of("kuayue")
        );
        return new LocatedDefinition<TrainPatch>(
                new DefinitionKey(TrainDataDrivenTypes.TRAINS, patch.id()),
                handler,
                patch,
                source,
                sourceOrder,
                LogicalPath.of("trains/ss3.json"),
                "/trains/" + declarationOrder,
                declarationOrder
        );
    }

    private static String minimalPatch(String id) {
        return """
                {"id":"%s","members":{"items":[]}}
                """.formatted(id);
    }

    private static String completePatch(String id) {
        return """
                {
                  "id":"%s",
                  "category":"locomotive",
                  "name_key":"train.kuayue.ss3.name",
                  "description_key":"train.kuayue.ss3.description",
                  "members":{
                    "blocks":[],
                    "items":[],
                    "item_tags":[],
                    "bogey_sizes":[],
                    "bogey_motions":[],
                    "bogey_models":[],
                    "bogey_blocks":[],
                    "bogey_styles":[],
                    "bogey_style_variants":[]
                  }
                }
                """.formatted(id);
    }

    private static final class RecordingContext implements ValidationContext {
        private final LocatedDefinition<?> current;
        private final Set<String> dependencies;
        private final List<LocatedDefinition<?>> visible;
        private final List<LocatedDefinition<?>> all;
        private final List<String> required = new ArrayList<>();
        private final List<Error> errors = new ArrayList<>();

        private RecordingContext(LocatedDefinition<?> current, Set<String> dependencies) {
            this(current, dependencies, List.of(current), List.of(current));
        }

        private RecordingContext(
                LocatedDefinition<?> current,
                Set<String> dependencies,
                List<LocatedDefinition<?>> visible,
                List<LocatedDefinition<?>> all
        ) {
            this.current = current;
            this.dependencies = Set.copyOf(dependencies);
            this.visible = List.copyOf(visible);
            this.all = List.copyOf(all);
        }

        @Override
        public String hostModId() {
            return TrainDataDrivenTypes.HOST_MOD_ID;
        }

        @Override
        public SourceDescriptor source() {
            return current.source();
        }

        @Override
        public int sourceOrder() {
            return current.sourceOrder();
        }

        @Override
        public LocatedDefinition<?> currentDefinition() {
            return current;
        }

        @Override
        public Set<String> directDependencies() {
            return dependencies;
        }

        @Override
        public Optional<LocatedDefinition<?>> find(DefinitionKey key) {
            List<LocatedDefinition<?>> matches = findAllVisible(key);
            return matches.size() == 1 ? Optional.of(matches.getFirst()) : Optional.empty();
        }

        @Override
        public List<LocatedDefinition<?>> findAllVisible(DefinitionKey key) {
            return visible.stream().filter(value -> key.equals(value.key())).toList();
        }

        @Override
        public List<LocatedDefinition<?>> findAll(DefinitionKey key) {
            return all.stream().filter(value -> key.equals(value.key())).toList();
        }

        @Override
        public List<LocatedDefinition<?>> findAll(String typeDomain) {
            return all.stream()
                    .filter(value -> value.typeName().equals(typeDomain))
                    .toList();
        }

        @Override
        public boolean canReference(DefinitionKey key) {
            return visible.stream().anyMatch(value -> key.equals(value.key()));
        }

        @Override
        public boolean requireDefinition(DefinitionKey key, String relativeJsonPointer) {
            required.add(key.typeDomain() + ":" + key.id());
            return true;
        }

        @Override
        public boolean resourceExists(ResourceDomain domain, ResourceLocation location) {
            return true;
        }

        @Override
        public boolean factoryAvailable(FactoryType factoryType, String factoryId) {
            return true;
        }

        @Override
        public boolean factorySupports(
                FactoryType factoryType,
                String factoryId,
                Class<?> capability
        ) {
            return true;
        }

        @Override
        public boolean isReserved(String symbol) {
            return false;
        }

        @Override
        public Set<String> reservedSymbols() {
            return Set.of();
        }

        @Override
        public RegistryClaim claim(String registry, ResourceLocation id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void error(
                BuildDiagnostic.Code code,
                String relativeJsonPointer,
                String message,
                String suggestion
        ) {
            errors.add(new Error(code, relativeJsonPointer, message));
        }

        @Override
        public void warning(
                BuildDiagnostic.Code code,
                String relativeJsonPointer,
                String message,
                String suggestion
        ) {
        }

        private record Error(
                BuildDiagnostic.Code code,
                String pointer,
                String message
        ) {
        }
    }
}
