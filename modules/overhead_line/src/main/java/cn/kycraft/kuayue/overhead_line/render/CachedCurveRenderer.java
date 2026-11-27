package cn.kycraft.kuayue.overhead_line.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public final class CachedCurveRenderer {
    private static final RenderType TYPE = RenderType.entitySolid(
            ResourceLocation.withDefaultNamespace("textures/misc/white.png"));
    private static final int WIRE_COLOR = 0xFF50545A;

    private CachedCurveRenderer() {}

    public static RenderType renderType() {
        return TYPE;
    }

    public static void render(RenderCurve curve, PoseStack.Pose pose, VertexConsumer out,
                              Vec3 camera, ClientLevel level) {
        for (RenderCurve.Segment segment : curve.segments()) {
            int light = LevelRenderer.getLightColor(level, segment.lightPos);
            quad(out, pose, segment, camera, light, segment.up, -1, 0, 1, 5, 4);
            quad(out, pose, segment, camera, light, segment.right, 1, 1, 2, 6, 5);
            quad(out, pose, segment, camera, light, segment.up, 1, 2, 3, 7, 6);
            quad(out, pose, segment, camera, light, segment.right, -1, 3, 0, 4, 7);
            quad(out, pose, segment, camera, light, segment.tangent, -1, 3, 2, 1, 0);
            quad(out, pose, segment, camera, light, segment.tangent, 1, 4, 5, 6, 7);
        }
    }

    private static void quad(VertexConsumer out, PoseStack.Pose pose, RenderCurve.Segment segment,
                             Vec3 camera, int light, Vec3 normal, int sign, int a, int b, int c, int d) {
        float nx = (float) (normal.x * sign);
        float ny = (float) (normal.y * sign);
        float nz = (float) (normal.z * sign);
        vertex(out, pose, segment.corner(a), camera, light, nx, ny, nz);
        vertex(out, pose, segment.corner(b), camera, light, nx, ny, nz);
        vertex(out, pose, segment.corner(c), camera, light, nx, ny, nz);
        vertex(out, pose, segment.corner(d), camera, light, nx, ny, nz);
    }

    private static void vertex(VertexConsumer out, PoseStack.Pose pose, Vec3 point, Vec3 camera,
                               int light, float nx, float ny, float nz) {
        out.addVertex(pose, (float) (point.x - camera.x), (float) (point.y - camera.y),
                        (float) (point.z - camera.z))
                .setColor(WIRE_COLOR)
                .setUv(0.5f, 0.5f)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }
}
