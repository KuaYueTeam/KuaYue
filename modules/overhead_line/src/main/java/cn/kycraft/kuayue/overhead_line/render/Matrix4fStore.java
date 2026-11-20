package cn.kycraft.kuayue.overhead_line.render;

import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.nio.FloatBuffer;
import java.util.Iterator;

public class Matrix4fStore implements Iterable<Matrix4f>{
    private final FloatBuffer buffer;
    private final int size;
    private final Matrix4f handler;

    public Matrix4fStore(FloatBuffer buffer, int size) {
        if(buffer.capacity() < 16 * size) {
            throw new IndexOutOfBoundsException(String.format("Invalid PointStack cache result: should the buffer capacity %d, %d got." , buffer.capacity(), size * 16));
        }
        this.buffer = buffer;
        this.size = size;
        this.handler = new Matrix4f();
    }

    public void write(int needle, Matrix4f matrix){
        matrix.get(buffer.slice(needle * 16,16));
    }

    public Matrix4f getHandler(){return handler;};

    public void load(int needle){handler.set(buffer.slice(needle * 16, 16));}

    @Override
    public @NotNull Iterator<Matrix4f> iterator() {
        return new Iterator<Matrix4f>() {
            private int index = 0;
            @Override
            public boolean hasNext() {
                return index < size;
            }

            @Override
            public Matrix4f next() {
                handler.set(buffer.slice(index++ * 16,16));
                return handler;
            }
        };
    }

    public int getSize() {
        return size;
    }

    public AABB getBoundingBox(){
        AABB aabb = null;
        Vector3f identify = new Vector3f(0,0,0);
        for (Matrix4f matrix4f : this){
            AABB vertexAABB = getBoundingBoxOf(this.getHandler(),identify);
            if (aabb == null){aabb = vertexAABB;}else {aabb=aabb.minmax(vertexAABB);}
        }
        return aabb;
    }

    public AABB[] getAllBoundingBox(){
        AABB[] aabbs = new AABB[size];
        Vector3f identify = new Vector3f(0,0,0);
        for (int i = 0; i < size; i++){
            AABB vertexAABB = getBoundingBoxOf(this.getHandler(),identify);
            aabbs[i] = vertexAABB;
        }
        return aabbs;
    }

    public static AABB getBoundingBoxOf(Matrix4f matrix4f,Vector3f identify){
        float w =matrix4f.m33();
        Vector3f position = new Vector3f(matrix4f.m30()/w,matrix4f.m31()/w,matrix4f.m33()/w);
        Matrix3f rotation_scale = new Matrix3f(matrix4f).scale(1.0f/w);

        AABB out_bounding_box = null;
        for (int i = 0; i <= 1 ;i++){
            for (int j = 0;j <= 1; j++){
                for (int k = 0;k <= 1; k++){
                    identify.set(i,j,k);
                    rotation_scale.transform(identify);
                    AABB vertexAABB = new AABB(position.x,position.y, position.z,
                            position.x + identify.x,
                            position.y + identify.y,
                            position.z + identify.z);
                    out_bounding_box = out_bounding_box == null ? vertexAABB : out_bounding_box.minmax(vertexAABB);
                }
            }
        }
        return out_bounding_box;
    }

    public Matrix4f get(Matrix4f matrixHandle, int i){
        matrixHandle.set(buffer.slice(i * 16, 16));
        return matrixHandle;
    }
}
