package cn.kycraft.kuayue.parts.bogeys.data;

import com.google.gson.JsonObject;
import lib.kasuga.registration.data_driven.TypeHandler;
import lib.kasuga.registration.data_driven.context.BuildContext;
import lib.kasuga.registration.data_driven.context.RegBuildContext;
import lib.kasuga.registration.data_driven.dedup.EffectiveId;
import lib.kasuga.registration.data_driven.pipeline.BuildDiagnostic;
import lib.kasuga.registration.data_driven.pipeline.DefinitionKey;
import lib.kasuga.registration.data_driven.pipeline.HandlerCapability;
import lib.kasuga.registration.data_driven.pipeline.LocatedDefinition;
import lib.kasuga.registration.data_driven.validation.ValidationContext;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Contributes variants from one content owner to a style owned by another pack.
 */
public final class BogeyStyleVariantsTypeHandler
        implements TypeHandler<BogeyStyleVariantsDefinition> {
    private final BogeyDefinitionParser parser = new BogeyDefinitionParser();

    @Override
    public String getTypeName() {
        return BogeyDataDrivenTypes.STYLE_VARIANTS;
    }

    @Override
    public int getPhase() {
        return 51;
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
    public void validate(
            BogeyStyleVariantsDefinition definition,
            ValidationContext context
    ) {
        DefinitionKey styleKey = styleKey(context, definition.style());
        context.requireDefinition(styleKey, "/style");

        Set<ResourceLocation> localSizes = new LinkedHashSet<>();
        Set<ResourceLocation> localBlocks = new LinkedHashSet<>();
        for (int index = 0; index < definition.variants().size(); index++) {
            BogeyStyleDefinition.Variant variant = definition.variants().get(index);
            String pointer = "/variants/" + index;
            ResourceLocation sizeId = ResourceLocation.parse(variant.size());
            ResourceLocation blockId = ResourceLocation.parse(variant.block());

            if (!localSizes.add(sizeId)) {
                context.error(
                        BuildDiagnostic.Code.CONTENT_INVALID,
                        pointer + "/size",
                        "Bogey style contribution configures size twice: " + sizeId,
                        "Use each size at most once across the target style."
                );
            }
            if (!localBlocks.add(blockId)) {
                context.error(
                        BuildDiagnostic.Code.CONTENT_INVALID,
                        pointer + "/block",
                        "Bogey style contribution configures block twice: " + blockId,
                        "Use each block at most once across the target style."
                );
            }

            context.requireDefinition(new DefinitionKey(
                    BogeyDataDrivenTypes.SIZES,
                    EffectiveId.of(context.hostModId(), variant.size())
            ), pointer + "/size");
            boolean blockVisible = context.requireDefinition(new DefinitionKey(
                    BogeyDataDrivenTypes.BLOCKS,
                    EffectiveId.of(context.hostModId(), variant.block())
            ), pointer + "/block");
            context.requireDefinition(new DefinitionKey(
                    BogeyDataDrivenTypes.MODELS,
                    EffectiveId.of(context.hostModId(), variant.model())
            ), pointer + "/model");

            var blockCandidate = context.find(new DefinitionKey(
                    BogeyDataDrivenTypes.BLOCKS,
                    EffectiveId.of(context.hostModId(), variant.block())
            )).orElse(null);
            if (blockVisible && blockCandidate != null
                    && blockCandidate.definition() instanceof BogeyBlockDefinition block) {
                for (Conflict conflict : blockConflicts(
                        context.hostModId(),
                        definition.style(),
                        index,
                        variant,
                        block
                )) {
                    context.error(
                            BuildDiagnostic.Code.CONTENT_INVALID,
                            conflict.pointer(),
                            conflict.message(),
                            "Keep the block size and default style aligned with its variant."
                    );
                }
            }
        }

        var styleDefinition = context.find(styleKey).orElse(null);
        BogeyStyleDefinition baseStyle = styleDefinition != null
                && styleDefinition.definition() instanceof BogeyStyleDefinition visible
                ? visible
                : null;
        for (Conflict conflict : duplicateConflicts(
                context.hostModId(),
                definition,
                baseStyle,
                context.currentDefinition(),
                context.findAll(BogeyDataDrivenTypes.STYLE_VARIANTS)
        )) {
            context.error(
                    BuildDiagnostic.Code.CONTENT_INVALID,
                    conflict.pointer(),
                    conflict.message(),
                    "Use a size and block that are unique across every contribution."
            );
        }
    }

    @Override
    public BogeyStyleVariantsDefinition parse(JsonObject json) {
        return parser.parseStyleVariants(json);
    }

    @Override
    public void apply(
            BogeyStyleVariantsDefinition definition,
            BuildContext baseContext
    ) {
        if (!(baseContext instanceof RegBuildContext context)) {
            throw new IllegalArgumentException(getTypeName() + " requires RegBuildContext");
        }
        BogeyStyleRegistration style = context.getMeta(
                BogeyDataDrivenTypes.STYLE_ACCUMULATOR,
                definition.style()
        );
        if (style == null) {
            throw new IllegalArgumentException(
                    "Bogey style contribution " + definition.id()
                            + " targets unknown or inactive style " + definition.style()
            );
        }
        for (BogeyStyleDefinition.Variant variant : definition.variants()) {
            style.addVariant(
                    variant,
                    context,
                    "contribution " + definition.id()
            );
        }
    }

    @Override
    public DefinitionKey parentDefinition(BogeyStyleVariantsDefinition definition) {
        return new DefinitionKey(BogeyDataDrivenTypes.STYLES, definition.style());
    }

    @Override
    public String resolveIdentity(
            String modId,
            BogeyStyleVariantsDefinition definition
    ) {
        return EffectiveId.of(modId, definition.id());
    }

    static boolean declaresBlock(
            ValidationContext context,
            String style,
            ResourceLocation blockId
    ) {
        var styleCandidate = context.find(styleKey(context, style)).orElse(null);
        if (styleCandidate != null
                && styleCandidate.definition() instanceof BogeyStyleDefinition styleDefinition) {
            boolean declared = styleDefinition.variants().stream()
                    .map(variant -> ResourceLocation.parse(
                            canonicalId(context.hostModId(), variant.block())
                    ))
                    .anyMatch(blockId::equals);
            if (declared) {
                return true;
            }
        }

        return declaresBlock(
                context.hostModId(),
                style,
                blockId.toString(),
                context.findAll(BogeyDataDrivenTypes.STYLE_VARIANTS),
                key -> context.find(key).orElse(null)
        );
    }

    static boolean declaresBlock(
            String hostModId,
            String style,
            String blockId,
            List<LocatedDefinition<?>> candidates,
            Function<DefinitionKey, LocatedDefinition<?>> effectiveLookup
    ) {
        String styleId = canonicalId(hostModId, style);
        String canonicalBlockId = canonicalId(hostModId, blockId);
        for (LocatedDefinition<?> candidate : candidates) {
            if (candidate.key() == null) {
                continue;
            }
            LocatedDefinition<?> effective = effectiveLookup.apply(candidate.key());
            if (effective == null
                    || !(effective.definition()
                    instanceof BogeyStyleVariantsDefinition contribution)
                    || !canonicalId(hostModId, contribution.style())
                    .equals(styleId)) {
                continue;
            }
            boolean declared = contribution.variants().stream()
                    .map(variant -> canonicalId(hostModId, variant.block()))
                    .anyMatch(canonicalBlockId::equals);
            if (declared) {
                return true;
            }
        }
        return false;
    }

    static List<Conflict> duplicateConflicts(
            String hostModId,
            BogeyStyleVariantsDefinition definition,
            BogeyStyleDefinition baseStyle,
            LocatedDefinition<?> current,
            List<LocatedDefinition<?>> allContributions
    ) {
        List<Conflict> conflicts = new java.util.ArrayList<>();
        String styleId = definition.style();
        for (LocatedDefinition<?> candidate : allContributions) {
            if ((candidate.key() != null && current.key() != null
                    && candidate.key().equals(current.key()))
                    || compareOccurrence(candidate, current) >= 0
                    || !(candidate.definition()
                    instanceof BogeyStyleVariantsDefinition earlierContribution)
                    || !canonicalId(
                    hostModId,
                    earlierContribution.style()
            ).equals(styleId)) {
                continue;
            }
            for (BogeyStyleDefinition.Variant earlier : earlierContribution.variants()) {
                for (int index = 0; index < definition.variants().size(); index++) {
                    BogeyStyleDefinition.Variant variant = definition.variants().get(index);
                    if (earlier.size().equals(variant.size())) {
                        conflicts.add(new Conflict(
                                "/variants/" + index + "/size",
                                "Bogey style " + styleId + " size " + variant.size()
                                        + " is already supplied by contribution "
                                        + earlierContribution.id()
                        ));
                    }
                    if (earlier.block().equals(variant.block())) {
                        conflicts.add(new Conflict(
                                "/variants/" + index + "/block",
                                "Bogey style " + styleId + " block " + variant.block()
                                        + " is already supplied by contribution "
                                        + earlierContribution.id()
                        ));
                    }
                }
            }
        }
        if (baseStyle != null) {
            for (BogeyStyleDefinition.Variant earlier : baseStyle.variants()) {
                for (int index = 0; index < definition.variants().size(); index++) {
                    BogeyStyleDefinition.Variant variant = definition.variants().get(index);
                    if (earlier.size().equals(variant.size())) {
                        conflicts.add(new Conflict(
                                "/variants/" + index + "/size",
                                "Bogey style " + styleId + " size " + variant.size()
                                        + " is already supplied by base style "
                                        + baseStyle.id()
                        ));
                    }
                    if (earlier.block().equals(variant.block())) {
                        conflicts.add(new Conflict(
                                "/variants/" + index + "/block",
                                "Bogey style " + styleId + " block " + variant.block()
                                        + " is already supplied by base style "
                                        + baseStyle.id()
                        ));
                    }
                }
            }
        }
        return List.copyOf(conflicts);
    }

    static List<Conflict> blockConflicts(
            String hostModId,
            String targetStyle,
            int variantIndex,
            BogeyStyleDefinition.Variant variant,
            BogeyBlockDefinition block
    ) {
        List<Conflict> conflicts = new java.util.ArrayList<>();
        if (!canonicalId(hostModId, block.size()).equals(
                canonicalId(hostModId, variant.size())
        )) {
            conflicts.add(new Conflict(
                    "/variants/" + variantIndex + "/size",
                    "Variant size does not match block " + block.id() + " size"
            ));
        }
        if (!canonicalId(hostModId, block.defaultStyle()).equals(
                canonicalId(hostModId, targetStyle)
        )) {
            conflicts.add(new Conflict(
                    "/variants/" + variantIndex + "/block",
                    "Block " + block.id() + " belongs to another default style"
            ));
        }
        return List.copyOf(conflicts);
    }

    private static DefinitionKey styleKey(
            ValidationContext context,
            String style
    ) {
        return new DefinitionKey(
                BogeyDataDrivenTypes.STYLES,
                EffectiveId.of(context.hostModId(), style)
        );
    }

    private static String canonicalId(String modId, String id) {
        return EffectiveId.of(modId, id);
    }

    static int compareOccurrence(
            LocatedDefinition<?> left,
            LocatedDefinition<?> right
    ) {
        int sourceOrder = Integer.compare(left.sourceOrder(), right.sourceOrder());
        if (sourceOrder != 0) {
            return sourceOrder;
        }
        int declarationOrder = Integer.compare(
                left.declarationOrder(),
                right.declarationOrder()
        );
        if (declarationOrder != 0) {
            return declarationOrder;
        }
        int path = left.logicalPath().value().compareTo(right.logicalPath().value());
        if (path != 0) {
            return path;
        }
        return left.jsonPointer().compareTo(right.jsonPointer());
    }

    record Conflict(String pointer, String message) {
    }

}
