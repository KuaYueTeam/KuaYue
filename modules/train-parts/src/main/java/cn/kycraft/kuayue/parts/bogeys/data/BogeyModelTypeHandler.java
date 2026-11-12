package cn.kycraft.kuayue.parts.bogeys.data;

import cn.kycraft.kuayue.parts.bogeys.core.BogeyModelPlan;
import com.google.gson.JsonObject;
import lib.kasuga.registration.data_driven.TypeHandler;
import lib.kasuga.registration.data_driven.context.BuildContext;
import lib.kasuga.registration.data_driven.dedup.EffectiveId;
import lib.kasuga.rendering.models.uml.math.Transform;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Compiles ordered model part and transform definitions from {@code bogey_models}.
 */
public final class BogeyModelTypeHandler implements TypeHandler<BogeyModelDefinition> {
    private final BogeyDefinitionParser parser = new BogeyDefinitionParser();

    @Override
    public String getTypeName() {
        return BogeyDataDrivenTypes.MODELS;
    }

    @Override
    public int getPhase() {
        return 30;
    }

    @Override
    public BogeyModelDefinition parse(JsonObject json) {
        return parser.parseModel(json);
    }

    @Override
    public void apply(BogeyModelDefinition definition, BuildContext context) {
        List<BogeyModelPlan.Part> parts = new ArrayList<>();
        for (BogeyModelDefinition.Part part : definition.parts()) {
            BogeyModelPlan.Motion motion = null;
            if (part.motion() != null) {
                motion = context.getMeta(
                        BogeyDataDrivenTypes.MOTION_PROFILE,
                        part.motion()
                );
                if (motion == null) {
                    throw new IllegalArgumentException(
                            "Model " + definition.id() + " part " + part.id()
                                    + " references unknown motion " + part.motion()
                    );
                }
            }
            parts.add(new BogeyModelPlan.Part(
                    part.id(),
                    ResourceLocation.parse(part.model()),
                    compile(part.local()),
                    motion
            ));
        }
        BogeyModelPlan plan = new BogeyModelPlan(
                ResourceLocation.parse(definition.id()),
                definition.yOffset(),
                parts
        );
        context.putMeta(BogeyDataDrivenTypes.MODEL_DEFINITION, definition.id(), definition);
        context.putMeta(BogeyDataDrivenTypes.MODEL_PLAN, definition.id(), plan);
    }

    @Override
    public String resolveIdentity(String modId, BogeyModelDefinition definition) {
        return EffectiveId.of(modId, definition.id());
    }

    private static Transform compile(List<BogeyModelDefinition.LocalOperation> operations) {
        Transform transform = new Transform();
        for (BogeyModelDefinition.LocalOperation operation : operations) {
            if (operation.translate() != null) {
                List<Float> value = operation.translate();
                transform.translate(value.get(0), value.get(1), value.get(2));
            } else if (operation.rotate() != null) {
                List<Float> value = operation.rotate();
                transform.rotate(value.get(0), value.get(1), value.get(2), true);
            } else {
                List<Float> value = operation.scale();
                transform.scale(value.get(0), value.get(1), value.get(2));
            }
        }
        return transform;
    }
}
