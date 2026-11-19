package cn.kycraft.kuayue.parts.bogeys.data;

import com.google.gson.JsonObject;
import lib.kasuga.registration.data_driven.TypeHandler;
import lib.kasuga.registration.data_driven.context.BuildContext;
import lib.kasuga.registration.data_driven.context.JsonRegistryGroup;
import lib.kasuga.registration.data_driven.context.RegBuildContext;
import lib.kasuga.registration.data_driven.dedup.EffectiveId;
import lib.kasuga.registration.data_driven.pipeline.BuildDiagnostic;
import lib.kasuga.registration.data_driven.pipeline.DefinitionKey;
import lib.kasuga.registration.data_driven.pipeline.HandlerCapability;
import lib.kasuga.registration.data_driven.pipeline.RegistryClaim;
import lib.kasuga.registration.data_driven.validation.ValidationContext;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Set;

/**
 * Creates Create bogey styles and connects their JSON variants to compiled model plans.
 */
public final class BogeyStyleTypeHandler implements TypeHandler<BogeyStyleDefinition> {
    private final BogeyDefinitionParser parser = new BogeyDefinitionParser();

    @Override
    public String getTypeName() {
        return BogeyDataDrivenTypes.STYLES;
    }

    @Override
    public int getPhase() {
        return 50;
    }

    @Override
    public Set<HandlerCapability> capabilities() {
        return Set.of(
                HandlerCapability.STRICT_PREFLIGHT,
                HandlerCapability.REGISTRY_CLAIMS
        );
    }

    @Override
    public boolean strictValidationImplemented() {
        return true;
    }

    @Override
    public void validate(BogeyStyleDefinition definition, ValidationContext context) {
        ResourceLocation styleId = ResourceLocation.parse(definition.id());
        ResourceLocation cycleId = ResourceLocation.parse(definition.cycleGroup());
        if (styleId.getPath().equals(cycleId.getPath())) {
            context.error(
                    BuildDiagnostic.Code.CONTENT_INVALID,
                    "/cycle_group",
                    "Bogey style path must differ from cycle path",
                    "Choose a distinct cycle group path."
            );
        }
        Set<ResourceLocation> configuredSizes = new java.util.LinkedHashSet<>();
        Set<ResourceLocation> configuredBlocks = new java.util.LinkedHashSet<>();
        for (int index = 0; index < definition.variants().size(); index++) {
            BogeyStyleDefinition.Variant variant = definition.variants().get(index);
            String pointer = "/variants/" + index;
            ResourceLocation sizeId = ResourceLocation.parse(variant.size());
            if (!configuredSizes.add(sizeId)) {
                context.error(
                        BuildDiagnostic.Code.CONTENT_INVALID,
                        pointer + "/size",
                        "Bogey style configures size twice: " + sizeId,
                        "Use each size at most once."
                );
            }
            ResourceLocation blockId = ResourceLocation.parse(variant.block());
            if (!configuredBlocks.add(blockId)) {
                context.error(
                        BuildDiagnostic.Code.CONTENT_INVALID,
                        pointer + "/block",
                        "Bogey style configures block twice: " + blockId,
                        "Use each bogey block at most once per style."
                );
            }
            context.requireDefinition(
                    new DefinitionKey(
                            BogeyDataDrivenTypes.SIZES,
                            EffectiveId.of(context.hostModId(), variant.size())
                    ),
                    pointer + "/size"
            );
            context.requireDefinition(
                    new DefinitionKey(
                            BogeyDataDrivenTypes.BLOCKS,
                            EffectiveId.of(context.hostModId(), variant.block())
                    ),
                    pointer + "/block"
            );
            context.requireDefinition(
                    new DefinitionKey(
                            BogeyDataDrivenTypes.MODELS,
                            EffectiveId.of(context.hostModId(), variant.model())
                    ),
                    pointer + "/model"
            );
            var blockCandidate = context.find(
                    new DefinitionKey(
                            BogeyDataDrivenTypes.BLOCKS,
                            EffectiveId.of(context.hostModId(), variant.block())
                    )
            ).orElse(null);
            if (blockCandidate != null
                    && blockCandidate.definition() instanceof BogeyBlockDefinition block) {
                if (!ResourceLocation.parse(block.size()).equals(sizeId)) {
                    context.error(
                            BuildDiagnostic.Code.CONTENT_INVALID,
                            pointer + "/size",
                            "Variant size does not match block " + block.id() + " size",
                            "Use the size declared by the referenced bogey block."
                    );
                }
                if (!ResourceLocation.parse(block.defaultStyle()).equals(styleId)) {
                    context.error(
                            BuildDiagnostic.Code.CONTENT_INVALID,
                            pointer + "/block",
                            "Block " + block.id() + " belongs to another default style",
                            "Use a block whose default_style is " + styleId + "."
                    );
                }
            }
        }
        if (definition.registryGroup() != null) {
            context.requireDefinition(
                    new DefinitionKey("registry_groups", definition.registryGroup()),
                    "/registry_group"
            );
        }
    }

    @Override
    public List<RegistryClaim> claims(
            BogeyStyleDefinition definition,
            ValidationContext context
    ) {
        ResourceLocation style = ResourceLocation.parse(
                EffectiveId.of(context.hostModId(), definition.id()));
        return List.of(context.claim("create:bogey_style", style));
    }

    @Override
    public BogeyStyleDefinition parse(JsonObject json) {
        return parser.parseStyle(json);
    }

    @Override
    public void apply(BogeyStyleDefinition definition, BuildContext baseContext) {
        if (!(baseContext instanceof RegBuildContext context)) {
            throw new IllegalArgumentException(getTypeName() + " requires RegBuildContext");
        }

        ResourceLocation styleId = ResourceLocation.parse(definition.id());
        ResourceLocation cycleId = ResourceLocation.parse(definition.cycleGroup());
        if (styleId.getPath().equals(cycleId.getPath())) {
            throw new IllegalArgumentException(
                    "Bogey style path must differ from cycle path: " + styleId
            );
        }

        JsonRegistryGroup parent = definition.registryGroup() == null
                ? context.getRootGroup()
                : context.getRegistryGroup(definition.registryGroup());
        if (definition.registryGroup() != null && parent == null) {
            throw new IllegalArgumentException(
                    "Unknown registry_group '" + definition.registryGroup()
                            + "' for bogey style " + definition.id()
            );
        }

        BogeyStyleRegistration registration = BogeyStyleRegistration.create(
                definition,
                styleId,
                parent
        );
        Set<ResourceLocation> configuredSizes = new java.util.LinkedHashSet<>();
        for (BogeyStyleDefinition.Variant variant : definition.variants()) {
            ResourceLocation sizeId = ResourceLocation.parse(variant.size());
            if (!configuredSizes.add(sizeId)) {
                throw new IllegalArgumentException(
                        "Bogey style " + styleId + " configures size twice: " + sizeId
                );
            }
            registration.addVariant(
                    variant,
                    context,
                    "base style " + definition.id()
            );
        }

        context.putMeta(
                BogeyDataDrivenTypes.STYLE_ACCUMULATOR,
                definition.id(),
                registration
        );
        context.putMeta(BogeyDataDrivenTypes.STYLE_DEFINITION, definition.id(), definition);
    }

    @Override
    public String resolveIdentity(String modId, BogeyStyleDefinition definition) {
        return EffectiveId.of(modId, definition.id());
    }

}
