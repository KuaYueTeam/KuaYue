package cn.kycraft.kuayue.parts.bogeys.core;

import lib.kasuga.rendering.models.uml.math.Transform;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KuaYueBogeyProxyCompositionTest {
    @Test
    void placedRootAlignsTheModelWithTheBlockForwardAxis() {
        Transform blockRoot = new Transform().translate(10f, 20f, 30f);
        Vector3f point = BogeyHostAlignment.placedRoot(blockRoot)
                .apply(new Vector3f(1f, 2f, 1f));

        assertEquals(9f, point.x, 1.0e-6f);
        assertEquals(22f, point.y, 1.0e-6f);
        assertEquals(29f, point.z, 1.0e-6f);
    }
}
