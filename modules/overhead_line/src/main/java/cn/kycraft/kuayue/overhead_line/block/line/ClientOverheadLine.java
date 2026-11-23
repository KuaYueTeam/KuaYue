package cn.kycraft.kuayue.overhead_line.block.line;

import cn.kycraft.kuayue.overhead_line.render.RenderCurve;
import lib.kasuga.structure.LazyRecomputable;
import lib.kasuga.structure.Pair;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

public class ClientOverheadLine {
   OverheadLineRenderer renderType;
   Level level;
   Vec3 fromPosition;
   Vec3 toPosition;

    LazyRecomputable<RenderCurve> curve = LazyRecomputable.of(this::computeCurve);
    LazyRecomputable<AABB> boundingBox = LazyRecomputable.of(() -> {
        RenderCurve currentCurve = getCurve();
        return currentCurve == null ? null : currentCurve.getMatrix().getBoundingBox();
    });

    public ClientOverheadLine(OverheadLineRenderer renderType, Level level, Vec3 position1, Vec3 position2) {
        this.renderType = Objects.requireNonNull(renderType, "renderType");
        this.level = Objects.requireNonNull(level, "level");
        setPosition(position1, position2);
    }

    public void setPosition(Vec3 position1, Vec3 position2) {
        // The Vec3 overload compares the second endpoint's Y coordinate with itself.
        if (position1 != null && position2 != null && PositionComparator.compare(
                position1.x(), position1.y(), position1.z(),
                position2.x(), position2.y(), position2.z()) > 0) {
            this.fromPosition = position2;
            this.toPosition = position1;
        } else {
            this.fromPosition = position1;
            this.toPosition = position2;
        }
        clearCache();
    }

    public void setRenderType(OverheadLineRenderer renderer) {
        this.renderType = Objects.requireNonNull(renderer, "renderer");
        clearCache();
    }

    public OverheadLineRenderer getRenderType() {
        return renderType;
    }

    public RenderCurve getCurve() {
        return curve.get();
    }

    public AABB getBoundingBox() {
        return boundingBox.get();
    }

    public Pair<Vec3, Vec3> getPosition() {
        return Pair.of(fromPosition, toPosition);
    }

    private RenderCurve computeCurve() {
        if (fromPosition == null || toPosition == null) {
            return null;
        }
        return renderType.getRenderCurveFor(level, fromPosition, toPosition);
    }

    private void clearCache() {
        curve.clear();
        boundingBox.clear();
    }
}
