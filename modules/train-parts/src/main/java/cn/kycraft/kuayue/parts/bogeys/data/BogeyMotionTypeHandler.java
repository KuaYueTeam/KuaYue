package cn.kycraft.kuayue.parts.bogeys.data;

import com.google.gson.JsonObject;
import lib.kasuga.registration.data_driven.TypeHandler;
import lib.kasuga.registration.data_driven.context.BuildContext;
import lib.kasuga.registration.data_driven.dedup.EffectiveId;
import lib.kasuga.registration.data_driven.pipeline.HandlerCapability;
import lib.kasuga.registration.data_driven.validation.ValidationContext;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;

/**
 * Compiles reusable motion profiles from {@code bogey_motions}.
 */
public final class BogeyMotionTypeHandler implements TypeHandler<BogeyMotionDefinition> {
    private final BogeyDefinitionParser parser = new BogeyDefinitionParser();

    @Override
    public String getTypeName() {
        return BogeyDataDrivenTypes.MOTIONS;
    }

    @Override
    public int getPhase() {
        return 20;
    }

    @Override
    public Set<HandlerCapability> capabilities() {
        return Set.of(
                HandlerCapability.STRICT_PREFLIGHT,
                HandlerCapability.NON_REGISTRATION
        );
    }

    @Override
    public boolean strictValidationImplemented() {
        return true;
    }

    @Override
    public void validate(BogeyMotionDefinition definition, ValidationContext context) {
        if ("formula".equals(definition.type())) {
            var clip = ResourceLocation.parse(definition.clip());
            var clipFile = ResourceLocation.fromNamespaceAndPath(
                    clip.getNamespace(),
                    "animation_clips/" + clip.getPath() + ".json"
            );
            if (!context.resourceExists(ValidationContext.ResourceDomain.DATA, clipFile)) {
                context.error(
                        lib.kasuga.registration.data_driven.pipeline.BuildDiagnostic.Code
                                .MISSING_REFERENCE,
                        "/clip",
                        "Formula clip '" + clip + "' is not visible in the prepared data source",
                        "Add the indexed reload file or declare its owner as a direct dependency."
                );
            }
        }
    }

    @Override
    public BogeyMotionDefinition parse(JsonObject json) {
        return parser.parseMotion(json);
    }

    @Override
    public void apply(BogeyMotionDefinition definition, BuildContext context) {
        context.putMeta(
                BogeyDataDrivenTypes.MOTION_PROFILE,
                definition.id(),
                CompiledBogeyMotion.compile(definition)
        );
        context.putMeta(
                BogeyDataDrivenTypes.MOTION_DEFINITION,
                definition.id(),
                definition
        );
    }

    @Override
    public String resolveIdentity(String modId, BogeyMotionDefinition definition) {
        return EffectiveId.of(modId, definition.id());
    }
}
