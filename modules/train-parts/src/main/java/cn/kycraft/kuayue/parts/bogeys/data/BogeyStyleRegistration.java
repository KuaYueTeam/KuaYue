package cn.kycraft.kuayue.parts.bogeys.data;

import cn.kycraft.kuayue.parts.bogeys.core.BogeyModelPlan;
import cn.kycraft.kuayue.parts.bogeys.core.CommonBogeyBlock;
import cn.kycraft.kuayue.parts.bogeys.core.KuaYueBogeyModels;
import cn.kycraft.kuayue.parts.bogeys.core.KuaYueBogeyRenderers;
import com.simibubi.create.content.trains.bogey.BogeyStyle;
import lib.kasuga.create.registration.bogey.BogeyBlockReg;
import lib.kasuga.create.registration.bogey.BogeyGroupReg;
import lib.kasuga.create.registration.bogey.BogeySizeReg;
import lib.kasuga.registration.data_driven.context.JsonRegistryGroup;
import lib.kasuga.registration.data_driven.context.RegBuildContext;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Mutable aggregation state shared by one base style and its later contributions.
 *
 * <p>Create commits a {@link BogeyGroupReg} during the registration Prepare stage, after every
 * data-driven definition has been applied. Contributions may therefore add sizes and model plans
 * while the same group is waiting to commit.
 */
final class BogeyStyleRegistration {
    private final String styleDefinitionId;
    private final ResourceLocation styleId;
    private final JsonRegistryGroup parent;
    private final BogeyGroupReg group;
    private final BogeyStyleVariantAggregation aggregation;
    private final List<CompiledVariant> compiledVariants = new ArrayList<>();
    private boolean installed;

    private BogeyStyleRegistration(
            BogeyStyleDefinition definition,
            ResourceLocation styleId,
            JsonRegistryGroup parent
    ) {
        this.styleDefinitionId = definition.id();
        this.styleId = styleId;
        this.parent = parent;
        this.aggregation = new BogeyStyleVariantAggregation(styleId.toString());
        this.group = createGroup(definition, styleId);
        group.onCommitted(style -> registerModels(style, compiledVariants));
    }

    static BogeyStyleRegistration create(
            BogeyStyleDefinition definition,
            ResourceLocation styleId,
            JsonRegistryGroup parent
    ) {
        return new BogeyStyleRegistration(definition, styleId, parent);
    }

    void addVariant(
            BogeyStyleDefinition.Variant variant,
            RegBuildContext context,
            String source
    ) {
        aggregation.add(variant, source);
        ResourceLocation sizeId = ResourceLocation.parse(variant.size());

        BogeySizeReg size = context.getMeta(BogeyDataDrivenTypes.SIZE_REG, variant.size());
        BogeyBlockReg<CommonBogeyBlock> block =
                context.getMeta(BogeyDataDrivenTypes.BLOCK_REG, variant.block());
        BogeyBlockDefinition blockDefinition =
                context.getMeta(BogeyDataDrivenTypes.BLOCK_DEFINITION, variant.block());
        if (size == null) {
            throw new IllegalArgumentException(
                    "Style " + styleId + " references unknown size " + variant.size()
            );
        }
        if (block == null || blockDefinition == null) {
            throw new IllegalArgumentException(
                    "Style " + styleId + " references unknown block " + variant.block()
            );
        }
        if (block.getSizeReg() != size) {
            throw new IllegalArgumentException(
                    "Style " + styleId + " size " + variant.size()
                            + " does not match block " + variant.block()
            );
        }
        if (!blockDefinition.defaultStyle().equals(styleId.toString())) {
            throw new IllegalArgumentException(
                    "Block " + variant.block() + " default style is "
                            + blockDefinition.defaultStyle() + ", not " + styleId
            );
        }

        BogeyModelPlan model = context.getMeta(
                BogeyDataDrivenTypes.MODEL_PLAN,
                variant.model()
        );
        if (model == null) {
            throw new IllegalArgumentException(
                    "Style " + styleId + " references unknown model " + variant.model()
            );
        }
        KuaYueBogeyRenderers.Binding renderer = KuaYueBogeyRenderers.bind(sizeId, size);
        group.bogey(size, block, renderer.handle());
        compiledVariants.add(new CompiledVariant(size, model));
        install(context);
    }

    int variantCount() {
        return aggregation.size();
    }

    private void install(RegBuildContext context) {
        if (installed) {
            return;
        }
        group.setParent(parent);
        context.putReg(BogeyDataDrivenTypes.STYLES, styleDefinitionId, group);
        context.putMeta(BogeyDataDrivenTypes.STYLE_REG, styleDefinitionId, group);
        installed = true;
    }

    private static BogeyGroupReg createGroup(
            BogeyStyleDefinition definition,
            ResourceLocation styleId
    ) {
        ResourceLocation cycleId = ResourceLocation.parse(definition.cycleGroup());
        return new BogeyGroupReg(styleId.getPath(), cycleId.getPath())
                .translationKey(definition.translationKey())
                .withProperty(ResourceLocation.class, id -> {
                    if (id.getPath().equals(styleId.getPath())) {
                        return styleId;
                    }
                    if (id.getPath().equals(cycleId.getPath())) {
                        return cycleId;
                    }
                    return id;
                });
    }

    private static void registerModels(
            BogeyStyle style,
            List<CompiledVariant> variants
    ) {
        for (CompiledVariant variant : variants) {
            KuaYueBogeyModels.register(
                    style,
                    variant.size().getEntry(),
                    variant.model()
            );
        }
    }

    private record CompiledVariant(
            BogeySizeReg size,
            BogeyModelPlan model
    ) {
    }
}
