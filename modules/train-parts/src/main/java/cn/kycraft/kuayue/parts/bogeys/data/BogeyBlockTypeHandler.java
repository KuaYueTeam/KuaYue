package cn.kycraft.kuayue.parts.bogeys.data;

import cn.kycraft.kuayue.parts.bogeys.core.CommonBogeyBlock;
import com.google.gson.JsonObject;
import com.simibubi.create.AllBogeyStyles;
import com.simibubi.create.content.trains.bogey.BogeyStyle;
import com.simibubi.create.content.trains.track.TrackMaterial;
import lib.kasuga.create.registration.bogey.BogeyBlockReg;
import lib.kasuga.create.registration.bogey.BogeySizeReg;
import lib.kasuga.registration.data_driven.TypeHandler;
import lib.kasuga.registration.data_driven.context.BuildContext;
import lib.kasuga.registration.data_driven.context.JsonRegistryGroup;
import lib.kasuga.registration.data_driven.context.RegBuildContext;
import lib.kasuga.registration.data_driven.dedup.EffectiveId;
import lib.kasuga.registration.data_driven.pipeline.BuildDiagnostic;
import lib.kasuga.registration.data_driven.pipeline.DefinitionKey;
import lib.kasuga.registration.data_driven.pipeline.HandlerCapability;
import lib.kasuga.registration.data_driven.pipeline.RegistryClaim;
import lib.kasuga.registration.data_driven.property.JsonPropertyParser;
import lib.kasuga.registration.data_driven.validation.ValidationContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Creates {@link CommonBogeyBlock} registrations from {@code bogey_blocks}.
 */
public final class BogeyBlockTypeHandler implements TypeHandler<BogeyBlockDefinition> {
    private final BogeyDefinitionParser parser = new BogeyDefinitionParser();

    @Override
    public String getTypeName() {
        return BogeyDataDrivenTypes.BLOCKS;
    }

    @Override
    public int getPhase() {
        return 40;
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
    public void validate(BogeyBlockDefinition definition, ValidationContext context) {
        context.requireDefinition(
                new DefinitionKey(
                        BogeyDataDrivenTypes.SIZES,
                        EffectiveId.of(context.hostModId(), definition.size())
                ),
                "/size"
        );
        context.requireDefinition(
                new DefinitionKey(
                        BogeyDataDrivenTypes.STYLES,
                        EffectiveId.of(context.hostModId(), definition.defaultStyle())
                ),
                "/default_style"
        );
        var style = context.find(new DefinitionKey(
                BogeyDataDrivenTypes.STYLES,
                EffectiveId.of(context.hostModId(), definition.defaultStyle())
        )).orElse(null);
        if (style != null && style.definition() instanceof BogeyStyleDefinition styleDefinition) {
            ResourceLocation blockId = ResourceLocation.parse(
                    EffectiveId.of(context.hostModId(), definition.id())
            );
            boolean declared = styleDefinition.variants().stream()
                    .map(variant -> ResourceLocation.parse(
                            EffectiveId.of(context.hostModId(), variant.block())
                    ))
                    .anyMatch(blockId::equals);
            if (!declared) {
                context.error(
                        BuildDiagnostic.Code.CONTENT_INVALID,
                        "/default_style",
                        "Bogey style " + styleDefinition.id()
                                + " does not declare this block in any variant",
                        "Add a matching variant or change default_style."
                );
            }
        }
        if (definition.registryGroup() != null) {
            context.requireDefinition(
                    new DefinitionKey("registry_groups", definition.registryGroup()),
                    "/registry_group"
            );
        }
        for (var issue : JsonPropertyParser.getInstance().validateStrict(definition.properties())) {
            context.error(
                    BuildDiagnostic.Code.CONTENT_INVALID,
                    "/properties" + issue.relativeJsonPointer(),
                    issue.message(),
                    "Fix the bogey block property according to the strict schema."
            );
        }
        String primary = definition.physics().primaryTrackType();
        if (!supportedTrackType(primary)) {
            context.error(
                    BuildDiagnostic.Code.CONTENT_INVALID,
                    "/physics/primary_track_type",
                    "Unsupported track type '" + primary + "'",
                    "Use 'standard' or 'create:standard'."
            );
        }
        for (int index = 0; index < definition.physics().validTrackTypes().size(); index++) {
            String type = definition.physics().validTrackTypes().get(index);
            if (!supportedTrackType(type)) {
                context.error(
                        BuildDiagnostic.Code.CONTENT_INVALID,
                        "/physics/valid_track_types/" + index,
                        "Unsupported track type '" + type + "'",
                        "Use 'standard' or 'create:standard'."
                );
            }
        }
    }

    @Override
    public List<RegistryClaim> claims(
            BogeyBlockDefinition definition,
            ValidationContext context
    ) {
        ResourceLocation id = ResourceLocation.parse(
                EffectiveId.of(context.hostModId(), definition.id()));
        return List.of(context.claim("minecraft:block", id));
    }

    @Override
    public BogeyBlockDefinition parse(JsonObject json) {
        return parser.parseBlock(json);
    }

    @Override
    public void apply(BogeyBlockDefinition definition, BuildContext baseContext) {
        if (!(baseContext instanceof RegBuildContext context)) {
            throw new IllegalArgumentException(getTypeName() + " requires RegBuildContext");
        }
        BogeySizeReg size = context.getMeta(BogeyDataDrivenTypes.SIZE_REG, definition.size());
        if (size == null) {
            throw new IllegalArgumentException(
                    "Unknown bogey size '" + definition.size() + "' for block " + definition.id()
            );
        }

        ResourceLocation blockId = ResourceLocation.parse(definition.id());
        ResourceLocation defaultStyleId = ResourceLocation.parse(definition.defaultStyle());
        Supplier<BogeyStyle> defaultStyle = () -> {
            BogeyStyle style = AllBogeyStyles.BOGEY_STYLES.get(defaultStyleId);
            if (style == null) {
                throw new IllegalStateException(
                        "Default bogey style has not been committed: " + defaultStyleId
                );
            }
            return style;
        };

        TrackMaterial.TrackType primaryType = trackType(
                definition.physics().primaryTrackType(),
                definition.id()
        );
        Set<TrackMaterial.TrackType> validTypes = definition.physics().validTrackTypes()
                .stream()
                .map(type -> trackType(type, definition.id()))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        List<Double> offset = definition.physics().connectorAnchorOffset();
        Vec3 connectorOffset = new Vec3(offset.get(0), offset.get(1), offset.get(2));

        BogeyBlockReg<CommonBogeyBlock> reg = new BogeyBlockReg<>(
                blockId.getPath(),
                size,
                (properties, resolvedSize) -> new CommonBogeyBlock(
                        properties,
                        resolvedSize,
                        defaultStyle,
                        primaryType,
                        validTypes,
                        definition.physics().wheelPointSpacing(),
                        connectorOffset,
                        definition.physics().allowSingleBogeyCarriage()
                )
        ).withProperty(ResourceLocation.class, ignored -> blockId);

        applyBlockProperties(definition.properties(), reg);
        JsonRegistryGroup parent = definition.registryGroup() == null
                ? context.getRootGroup()
                : context.getRegistryGroup(definition.registryGroup());
        if (definition.registryGroup() != null && parent == null) {
            throw new IllegalArgumentException(
                    "Unknown registry_group '" + definition.registryGroup()
                            + "' for bogey block " + definition.id()
            );
        }
        reg.setParent(parent);
        context.putReg(getTypeName(), definition.id(), reg);
        context.putMeta(BogeyDataDrivenTypes.BLOCK_REG, definition.id(), reg);
        context.putMeta(BogeyDataDrivenTypes.BLOCK_DEFINITION, definition.id(), definition);
        List<BogeyBlockReg<CommonBogeyBlock>> registrations =
                context.getMeta(BogeyDataDrivenTypes.RUNTIME,
                        BogeyDataDrivenTypes.BLOCK_REGISTRATIONS);
        if (registrations == null) {
            registrations = new ArrayList<>();
            context.putMeta(BogeyDataDrivenTypes.RUNTIME,
                    BogeyDataDrivenTypes.BLOCK_REGISTRATIONS, registrations);
        }
        registrations.add(reg);
    }

    @Override
    public String resolveIdentity(String modId, BogeyBlockDefinition definition) {
        return EffectiveId.of(modId, definition.id());
    }

    private static void applyBlockProperties(
            JsonObject properties,
            BogeyBlockReg<CommonBogeyBlock> reg
    ) {
        for (Function<BlockBehaviour.Properties, BlockBehaviour.Properties> modifier
                : JsonPropertyParser.getInstance().parseBlockProperties(properties)) {
            reg.withProperty(BlockBehaviour.Properties.class, modifier);
        }
    }

    private static TrackMaterial.TrackType trackType(String raw, String blockId) {
        if ("standard".equals(raw) || "create:standard".equals(raw)) {
            return TrackMaterial.TrackType.STANDARD;
        }
        throw new IllegalArgumentException(
                "Unsupported track type '" + raw + "' for bogey block " + blockId
        );
    }

    private static boolean supportedTrackType(String raw) {
        return "standard".equals(raw) || "create:standard".equals(raw);
    }
}
