package cn.kycraft.kuayue.overhead_line.render;

import com.mojang.blaze3d.vertex.PoseStack;
import lib.kasuga.structure.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Iterator;

public class RenderCurve implements Iterable<Pair<Matrix4f,Integer>>{
    private final Matrix4fStore matrix;

    public RenderCurve(int size,ArrayList<Vec2> pantographAdjustVectors) {
        this.matrix = new Matrix4fStore(FloatBuffer.allocate(size * 16), size);
        this.pantographAdjustVectors = pantographAdjustVectors;
        this.lightStore = new int[size];
    }

    public Matrix4fStore getMatrix(){return matrix;};
    private final ArrayList<Vec2> pantographAdjustVectors;
    public final ArrayList<Vec2> getPantographAdjustVectors(){return pantographAdjustVectors;};
    private final int[] lightStore;

    @Override
    public @NotNull Iterator<Pair<Matrix4f, Integer>> iterator() {
        return new Iterator<Pair<Matrix4f, Integer>>() {
            Matrix4f matrixHandle = new Matrix4f();
            int i = 0;

            @Override
            public boolean hasNext() {return i < matrix.getSize();}

            @Override
            public Pair<Matrix4f, Integer> next() {return lib.kasuga.structure.Pair.of(matrix.get(matrixHandle,i),lightStore[i++]);}
        };
    }

    public Iterator<Vec2> getVec2Iterator(){return pantographAdjustVectors.iterator();};
    public static Builder create(){return new Builder();}

    public static class Builder{
        private final PoseStack pose;
        private final ArrayList<Matrix4f> matrix4fs = new ArrayList<>();
        private final ArrayList<Vec2> pantographAdjustVectors = new ArrayList<>();
        Builder() {pose = new PoseStack();}
        public PoseStack getPoseStack(){return pose;}
        public void store(){matrix4fs.add(pose.last().pose());}
        public void store(Vec2 vec2){pantographAdjustVectors.add(vec2);this.store();}

        public RenderCurve build(Level level){
            RenderCurve curve = new RenderCurve(matrix4fs.size(),pantographAdjustVectors);
            for (int i = 0; i < matrix4fs.size(); i++){
                curve.matrix.write(i,matrix4fs.get(i));
            }
            buildLightLevel(curve,level);
            return curve;
        }
    }

    protected static int getLightAt(Level level, BlockPos blockPos){
        return level.getLightEngine().getRawBrightness(blockPos,0);
    }

    protected static void buildLightLevel(RenderCurve curve, Level level){
        AABB[] boxes = curve.matrix.getAllBoundingBox();
        for (int i = 0; i < curve.matrix.getAllBoundingBox().length;i++){
            curve.lightStore[i] = getLightAt(level,BlockPos.containing(boxes[i].getCenter()));
        }
    }

}
