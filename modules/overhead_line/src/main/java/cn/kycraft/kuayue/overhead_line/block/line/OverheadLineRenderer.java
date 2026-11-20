package cn.kycraft.kuayue.overhead_line.block.line;

import cn.kycraft.kuayue.overhead_line.render.RenderCurve;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public interface OverheadLineRenderer {
    RenderCurve getRenderCurveFor(Level level, Vec3 from, Vec3 to);

    void render(
            RenderCurve curve,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int overlay
    );
}
