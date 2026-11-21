package cn.kycraft.kuayue.overhead_line.client;

import cn.kycraft.kuayue.overhead_line.block.support.variants.ModelledOverheadSupportBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import lib.kasuga.rendering.models.mc.java_and_bedrock.data.MCTextureData;
import lib.kasuga.rendering.models.mc.registry.PipelineRegistry;
import lib.kasuga.rendering.models.uml.structure.Model;
import lib.kasuga.rendering.models.uml.structure.basic.Mesh;
import lib.kasuga.rendering.models.uml.structure.basic.Vertex;
import lib.kasuga.rendering.models.uml.structure.material.Material;
import lib.kasuga.rendering.models.uml.structure.material.Sprite;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector2f;
import org.joml.Vector3f;

public final class OverheadModelItemRenderer extends BlockEntityWithoutLevelRenderer {
    public OverheadModelItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poses,
                             MultiBufferSource buffers, int light, int overlay) {
        if (!(stack.getItem() instanceof BlockItem item)
                || !(item.getBlock() instanceof ModelledOverheadSupportBlock block)
                || !block.model().getPath().endsWith(".obj") || !PipelineRegistry.isInitialized()) {
            return;
        }

        var pipeline = PipelineRegistry.resolve(block.model());
        Model model = pipeline == null ? null : pipeline.getModel(block.model());
        if (model == null) return;

        PoseStack.Pose pose = poses.last();
        Vector3f faceNormal = new Vector3f();
        Vector3f edgeA = new Vector3f();
        Vector3f edgeB = new Vector3f();
        for (Mesh face : model.getMeshes()) {
            Vertex[] vertices = face.getVertices();
            if (vertices.length < 3) continue;

            faceNormal.zero();
            Vector3f origin = vertices[0].getPosition();
            for (int i = 1; i < vertices.length - 1; i++) {
                edgeA.set(vertices[i].getPosition()).sub(origin);
                edgeB.set(vertices[i + 1].getPosition()).sub(origin);
                faceNormal.add(edgeA.cross(edgeB));
            }
            if (faceNormal.lengthSquared() == 0) continue;
            faceNormal.normalize();

            for (Material material : face.getMaterials()) {
                if (material.getSprites().isEmpty()) continue;
                Sprite sprite = material.getSprites().getFirst().getSprite(0);
                if (sprite.getTexture() == null || !(sprite.getTexture().getData() instanceof MCTextureData texture)) {
                    continue;
                }
                TextureAtlasSprite atlasSprite = texture.getSprite();
                if (atlasSprite == null) {
                    atlasSprite = texture.getTextureManager().getTextureAtlas().getSprite(MissingTextureAtlasSprite.getLocation());
                    if (atlasSprite == null) continue;
                }
                VertexConsumer out = ItemRenderer.getFoilBufferDirect(buffers,
                        RenderType.entityCutoutNoCull(atlasSprite.atlasLocation()), true, stack.hasFoil());
                if (vertices.length == 4) {
                    for (Vertex vertex : vertices) {
                        emit(out, pose, vertex, face, faceNormal, material, sprite, atlasSprite, light, overlay);
                    }
                } else {
                    for (int i = 1; i < vertices.length - 1; i++) {
                        emit(out, pose, vertices[0], face, faceNormal, material, sprite, atlasSprite, light, overlay);
                        emit(out, pose, vertices[i], face, faceNormal, material, sprite, atlasSprite, light, overlay);
                        emit(out, pose, vertices[i + 1], face, faceNormal, material, sprite, atlasSprite, light, overlay);
                        emit(out, pose, vertices[i + 1], face, faceNormal, material, sprite, atlasSprite, light, overlay);
                    }
                }
            }
        }
    }

    private static void emit(VertexConsumer out, PoseStack.Pose pose, Vertex vertex, Mesh face,
                             Vector3f faceNormal, Material material, Sprite sprite, TextureAtlasSprite atlasSprite,
                             int light, int overlay) {
        Vector2f uv = vertex.getUV(face, material);
        float u = uv == null ? 0 : Math.clamp(uv.x(), 0, 1);
        float v = uv == null ? 0 : Math.clamp(uv.y(), 0, 1);
        if (sprite.flipU) u = 1 - u;
        if (sprite.flipV) v = 1 - v;
        Vector3f normal = vertex.getNormals().get(face);
        if (normal == null || normal.lengthSquared() == 0) normal = faceNormal;
        out.addVertex(pose, vertex.getPosition())
                .setColor(sprite.color.x(), sprite.color.y(), sprite.color.z(), sprite.color.w())
                .setUv(atlasSprite.getU(u), atlasSprite.getV(v))
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(pose, normal.x(), normal.y(), normal.z());
    }
}
