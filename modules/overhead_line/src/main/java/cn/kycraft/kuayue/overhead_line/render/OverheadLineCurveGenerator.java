package cn.kycraft.kuayue.overhead_line.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Quaternionf;
import org.joml.Vector3f;


public class OverheadLineCurveGenerator {
    public static final Vec3 UP =new Vec3(0,1,0);

    public static RenderCurve straightLine(Level level,Vec3 a,Vec3 b,float r){
        RenderCurve.Builder builder = RenderCurve.create();
        prepareLine(builder,a,b.subtract(a),r);
        straightLine(builder,a,b,r);
        return builder.build(level);
    }

    public static void prepareLine(RenderCurve.Builder builder,Vec3 a,Vec3 dir,float r){
        PoseStack pose = builder.getPoseStack();
        pose.translate(a.x - 0.5 * r, a.y - 0.5 * r,a.z - 0.5 * r);
        Axis YP = angle -> new Quaternionf().rotationY(angle);
        pose.mulPose(Axis.YP.rotation((float) Math.atan2(dir.x,dir.z)));
    }

    public static void straightLine(RenderCurve.Builder builder, Vec3 a,Vec3 b,float r){
        Vec3 dir = b.subtract(a);

        float rotZ = - (float)Math.atan2(dir.y,Math.sqrt(dir.x * dir.x + dir.z * dir.z));
        Quaternionf q = Axis.XP.rotation(rotZ);
        Quaternionf qZ = Axis.ZP.rotation(rotZ);
        float distance = (float) a.distanceTo(b);

        Vector3f one = new Vector3f(1,1,0);
        one.rotate(qZ);
        one.mul(distance);
        Vec2 vecResult = new Vec2((float) Math.sqrt(one.x() * one.x() + one.z() * one.z()), one.y());

        PoseStack pose = builder.getPoseStack();
        pose.mulPose(q);
        pose.scale(r,r,distance);
        pose.translate(-0.5,-0.5,-0.5);

        builder.store(vecResult);
    }

//    public static void conicHangLine(RenderCurve.Builder builder ,Vec3 first,Vec3 last,float ) {
//
//    }
}
