package cn.kycraft.kuayue.parts.bogeys.data;

import cn.kycraft.kuayue.parts.bogeys.core.BogeyModelPlan;
import lib.kasuga.rendering.models.mc.proxies.ModelPart;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompiledBogeyMotionTest {
    @Test
    void wheelMotionBindsSpinInTheRequestedDomain() {
        CompiledBogeyMotion.WheelSpin motion =
                new CompiledBogeyMotion.WheelSpin(3.256f, -1f);
        BogeyModelPlan.BindingContext context = new BogeyModelPlan.BindingContext();

        ModelPart.Motion tickMotion = motion.bind("wheel", () -> 10f, false, context);
        assertEquals(ModelPart.TimeDomain.TICK, tickMotion.timeDomain());
        assertTrue(tickMotion instanceof ModelPart.Spin);
        ModelPart.Spin tickSpin = (ModelPart.Spin) tickMotion;
        assertEquals(3.256f, tickSpin.scale());

        ModelPart.Motion frameMotion = motion.bind("wheel", () -> 10f, true, context);
        assertEquals(ModelPart.TimeDomain.FRAME, frameMotion.timeDomain());
        assertTrue(frameMotion instanceof ModelPart.Spin);
        ModelPart.Spin frameSpin = (ModelPart.Spin) frameMotion;
        assertEquals(3.256f, frameSpin.scale());
    }

    @Test
    void rodRotationUsesCompleteTransformsInTheRequestedDomain() {
        CompiledBogeyMotion.RodRotation motion =
                new CompiledBogeyMotion.RodRotation(3.256f, 1f);
        BogeyModelPlan.BindingContext context = new BogeyModelPlan.BindingContext();

        assertEquals(
                ModelPart.TimeDomain.TICK,
                motion.bind("rod", () -> 20f, false, context).timeDomain()
        );
        assertEquals(
                ModelPart.TimeDomain.FRAME,
                motion.bind("rod", () -> 20f, true, context).timeDomain()
        );
    }
}
