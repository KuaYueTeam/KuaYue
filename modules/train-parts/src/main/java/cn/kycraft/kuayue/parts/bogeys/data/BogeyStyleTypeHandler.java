package cn.kycraft.kuayue.parts.bogeys.data;

import cn.kycraft.kuayue.parts.bogeys.core.BogeyModelPlan;
import cn.kycraft.kuayue.parts.bogeys.core.KuaYueBogeyModels;
import cn.kycraft.kuayue.parts.bogeys.core.KuaYueBogeyRenderers;
import com.google.gson.JsonObject;
import com.simibubi.create.content.trains.bogey.BogeyStyle;
import lib.kasuga.create.registration.bogey.BogeyBlockReg;
import lib.kasuga.create.registration.bogey.BogeyGroupReg;
import lib.kasuga.create.registration.bogey.BogeySizeReg;
import lib.kasuga.registration.data_driven.TypeHandler;
import lib.kasuga.registration.data_driven.context.BuildContext;
import lib.kasuga.registration.data_driven.context.JsonRegistryGroup;
import lib.kasuga.registration.data_driven.context.RegBuildContext;
import lib.kasuga.registration.data_driven.dedup.EffectiveId;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
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

        BogeyGroupReg group = new BogeyGroupReg(styleId.getPath(), cycleId.getPath())
                .translationKey(definition.translationKey())
                .withProperty(ResourceLocation.class, id -> {
                    if (id.getPath().equals(styleId.getPath())) {
                        return styleId;
                    }
                    if (id.getPath().equals(cycleId.getPath())) {
                        return cycleId;
                    }
                    return id;
                })
                .setParent(parent);

        List<CompiledVariant> variants = new ArrayList<>();
        Set<ResourceLocation> configuredSizes = new java.util.LinkedHashSet<>();
        for (BogeyStyleDefinition.Variant variant : definition.variants()) {
            ResourceLocation sizeId = ResourceLocation.parse(variant.size());
            if (!configuredSizes.add(sizeId)) {
                throw new IllegalArgumentException(
                        "Bogey style " + styleId + " configures size twice: " + sizeId
                );
            }
            BogeySizeReg size = context.getMeta(BogeyDataDrivenTypes.SIZE_REG, variant.size());
            BogeyBlockReg<cn.kycraft.kuayue.parts.bogeys.core.CommonBogeyBlock> block =
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
            variants.add(new CompiledVariant(size, model));
        }

        group.onCommitted(style -> registerModels(style, variants));
        BogeyRuntimeRegistration.ensure(context);
        context.putReg(getTypeName(), definition.id(), group);
        context.putMeta(BogeyDataDrivenTypes.STYLE_REG, definition.id(), group);
        context.putMeta(BogeyDataDrivenTypes.STYLE_DEFINITION, definition.id(), definition);
    }

    @Override
    public String resolveIdentity(String modId, BogeyStyleDefinition definition) {
        return EffectiveId.of(modId, definition.id());
    }

    private static void registerModels(BogeyStyle style, List<CompiledVariant> variants) {
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
