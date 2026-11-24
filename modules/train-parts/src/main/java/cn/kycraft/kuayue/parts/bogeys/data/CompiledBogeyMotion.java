package cn.kycraft.kuayue.parts.bogeys.data;

import cn.kycraft.kuayue.parts.bogeys.core.BogeyModelPlan;
import lib.kasuga.rendering.models.mc.proxies.ModelPart;
import lib.kasuga.rendering.models.uml.dynamic.animation.FormulaTransformSampler;
import lib.kasuga.rendering.models.uml.math.Transform;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

/**
 * Runtime implementation of one data-driven bogey motion profile.
 */
public sealed interface CompiledBogeyMotion extends BogeyModelPlan.Motion
        permits CompiledBogeyMotion.WheelSpin,
        CompiledBogeyMotion.RodRotation,
        CompiledBogeyMotion.Formula {

    /**
     * Compiles a validated motion definition.
     *
     * @param definition parsed motion definition
     * @return runtime motion profile
     */
    static CompiledBogeyMotion compile(BogeyMotionDefinition definition) {
        return switch (definition.type()) {
            case "wheel_spin" -> new WheelSpin(definition.scale(), definition.sign());
            case "rod_rotation" -> new RodRotation(definition.scale(), definition.sign());
            case "formula" -> new Formula(ResourceLocation.parse(definition.clip()));
            default -> throw new IllegalArgumentException(
                    "Unsupported bogey motion type: " + definition.type()
            );
        };
    }

    /**
     * Standard wheel rotation with a gear-ratio scale and direction.
     *
     * @param scale angular scale
     * @param sign  angular direction
     */
    record WheelSpin(float scale, float sign) implements CompiledBogeyMotion {
        @Override
        public ModelPart.Motion bind(
                String partId,
                Supplier<Float> wheelAngle,
                boolean frameDomain,
                BogeyModelPlan.BindingContext context
        ) {
            ModelPart.Spin.AngleSource source = () -> wheelAngle.get() * sign;
            return frameDomain
                    ? ModelPart.Spin.frame(source, scale)
                    : ModelPart.Spin.tick(source, scale);
        }
    }

    /**
     * Transmission-rod rotation around the local Z axis.
     *
     * @param scale angular scale
     * @param sign  angular direction
     */
    record RodRotation(float scale, float sign) implements CompiledBogeyMotion {
        @Override
        public ModelPart.Motion bind(
                String partId,
                Supplier<Float> wheelAngle,
                boolean frameDomain,
                BogeyModelPlan.BindingContext context
        ) {
            if (frameDomain) {
                return ModelPart.frameTransform(
                        () -> transform(wheelAngle, scale, sign)
                );
            }
            return ModelPart.tickTransform(
                    ignored -> transform(wheelAngle, scale, sign)
            );
        }

        private static Transform transform(
                Supplier<Float> wheelAngle,
                float scale,
                float sign
        ) {
            return new Transform().rotate(
                    0,
                    0,
                    wheelAngle.get() * sign * scale,
                    true
            );
        }
    }

    /**
     * Formula animation clip applied to one model part.
     *
     * @param clipId formula animation clip ID
     */
    record Formula(ResourceLocation clipId) implements CompiledBogeyMotion {
        /**
         * Creates and validates one formula motion profile.
         */
        public Formula {
            clipId = java.util.Objects.requireNonNull(clipId, "clipId");
        }

        @Override
        public ModelPart.Motion bind(
                String partId,
                Supplier<Float> wheelAngle,
                boolean frameDomain,
                BogeyModelPlan.BindingContext context
        ) {
            FormulaTransformSampler sampler = context.formulaSampler(clipId);
            if (frameDomain) {
                return ModelPart.frameFormula(
                        sampler,
                        partId,
                        input -> input.assignQuery("wheel_angle", wheelAngle.get())
                );
            }
            return ModelPart.tickFormula(
                    sampler,
                    partId,
                    input -> input.assignQuery("wheel_angle", wheelAngle.get())
            );
        }
    }
}
