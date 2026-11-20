package cn.kycraft.kuayue.train;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lib.kasuga.registration.data_driven.TypeHandler;
import lib.kasuga.registration.data_driven.context.BuildContext;
import lib.kasuga.registration.data_driven.dedup.EffectiveId;
import lib.kasuga.registration.data_driven.pipeline.BuildDiagnostic;
import lib.kasuga.registration.data_driven.pipeline.DefinitionKey;
import lib.kasuga.registration.data_driven.pipeline.HandlerCapability;
import lib.kasuga.registration.data_driven.pipeline.JsonPointers;
import lib.kasuga.registration.data_driven.pipeline.LocatedDefinition;
import lib.kasuga.registration.data_driven.validation.ValidationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Registers and validates KuaYue's overlay-only {@code trains} data-driven type.
 */
public final class TrainTypeHandler implements TypeHandler<TrainPatch> {
    private final TrainPatchParser parser = new TrainPatchParser();

    @Override
    public String getTypeName() {
        return TrainDataDrivenTypes.TRAINS;
    }

    @Override
    public int getPhase() {
        return TrainDataDrivenTypes.PHASE;
    }

    @Override
    public Set<HandlerCapability> capabilities() {
        return Set.of(
                HandlerCapability.STRICT_PREFLIGHT,
                HandlerCapability.NON_REGISTRATION,
                HandlerCapability.OVERLAY
        );
    }

    @Override
    public boolean strictValidationImplemented() {
        return true;
    }

    @Override
    public TrainPatch parse(JsonObject json) {
        return parser.parse(json);
    }

    @Override
    public void validate(TrainPatch definition, ValidationContext context) {
        validateDeclaredReferences(definition.declaredFields(), "", context);
        validateContributors(definition, context);
        validateEffectiveDefinition(definition, context);
    }

    @Override
    public void apply(TrainPatch definition, BuildContext context) {
        // The finalizer compiles all patches after every phase has completed.
    }

    @Override
    public String resolveIdentity(String modId, TrainPatch definition) {
        return EffectiveId.of(modId, definition.id());
    }

    private static void validateDeclaredReferences(
            JsonObject object,
            String pointer,
            ValidationContext context
    ) {
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            String field = entry.getKey();
            String childPointer = JsonPointers.append(pointer, field);
            JsonElement value = entry.getValue();

            if (TrainDataDrivenTypes.isHandlerReferenceField(field)) {
                JsonArray references = value.getAsJsonArray();
                for (int index = 0; index < references.size(); index++) {
                    String resourceId = references.get(index).getAsString();
                    context.requireDefinition(
                            new DefinitionKey(
                                    field,
                                    EffectiveId.of(context.hostModId(), resourceId)
                            ),
                            childPointer + "/" + index
                    );
                }
            } else if (value.isJsonObject()) {
                validateDeclaredReferences(value.getAsJsonObject(), childPointer, context);
            }
        }
    }

    private static void validateContributors(
            TrainPatch definition,
            ValidationContext context
    ) {
        DefinitionKey key = new DefinitionKey(
                TrainDataDrivenTypes.TRAINS,
                EffectiveId.of(context.hostModId(), definition.id())
        );
        LocatedDefinition<?> currentDefinition = context.currentDefinition();
        int currentDeclarationOrder = currentDefinition == null
                ? Integer.MAX_VALUE
                : currentDefinition.declarationOrder();
        String currentOwner = context.source().ownerId();
        int currentSourceOrder = context.sourceOrder();

        for (LocatedDefinition<?> candidate : context.findAllVisible(key)) {
            if (!(candidate.definition() instanceof TrainPatch other)
                    || !definition.id().equals(other.id())) {
                continue;
            }
            String otherOwner = candidate.source().ownerId();
            if (currentOwner.equals(otherOwner)
                    && candidate.declarationOrder() < currentDeclarationOrder) {
                context.error(
                        BuildDiagnostic.Code.CONTENT_INVALID,
                        "/id",
                        "Owner '" + currentOwner + "' declares train " + definition.id()
                                + " more than once",
                        "Declare each train at most once per owner."
                );
            }
        }

        for (LocatedDefinition<?> candidate : context.findAll(key)) {
            if (!(candidate.definition() instanceof TrainPatch other)
                    || !definition.id().equals(other.id())
                    || candidate.sourceOrder() >= currentSourceOrder) {
                continue;
            }
            String lowerOwner = candidate.source().ownerId();
            if (!currentOwner.equals(lowerOwner)
                    && !context.directDependencies().contains(lowerOwner)) {
                context.error(
                        BuildDiagnostic.Code.REFERENCE_NOT_DECLARED,
                        "/id",
                        "Train " + definition.id() + " overlays contributor owned by '"
                                + lowerOwner + "', but that owner is not a direct dependency",
                        "Declare pack dependency '" + lowerOwner
                                + "' before overriding its train."
                );
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void validateEffectiveDefinition(
            TrainPatch definition,
            ValidationContext context
    ) {
        DefinitionKey key = new DefinitionKey(
                TrainDataDrivenTypes.TRAINS,
                EffectiveId.of(context.hostModId(), definition.id())
        );
        List<LocatedDefinition<TrainPatch>> patches = new ArrayList<>();
        for (LocatedDefinition<?> candidate : context.findAllVisible(key)) {
            if (!(candidate.definition() instanceof TrainPatch patch)
                    || !definition.id().equals(patch.id())) {
                continue;
            }
            patches.add(new LocatedDefinition<TrainPatch>(
                    candidate.key(),
                    (TypeHandler<TrainPatch>) candidate.handler(),
                    patch,
                    candidate.source(),
                    candidate.sourceOrder(),
                    candidate.logicalPath(),
                    candidate.jsonPointer(),
                    candidate.declarationOrder()
            ));
        }
        try {
            new TrainOverlayMerger().merge(patches);
        } catch (TrainOverlayMerger.MergeException exception) {
            for (String violation : exception.violations()) {
                int separator = violation.indexOf(':');
                String pointer = separator < 0 ? "" : violation.substring(0, separator);
                context.error(
                        BuildDiagnostic.Code.CONTENT_INVALID,
                        pointer,
                        violation,
                        "Make the effective train complete after all lower-priority patches are applied."
                );
            }
        }
    }
}
