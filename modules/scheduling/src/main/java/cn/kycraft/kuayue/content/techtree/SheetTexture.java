package cn.kycraft.kuayue.content.techtree;

import com.lowdragmc.lowdraglib2.client.shader.LDLibRenderTypes;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * Draws a fixed UV slice of a sheet texture.
 *
 * <p>Only the {@link GUIContext} overload is implemented. {@code IGuiTexture}'s default implementation
 * forwards to the {@code GuiGraphics} variant while injecting {@code localMouseX/localMouseY} as the
 * origin, which made every slice follow the cursor. The GUIContext path hands over the element
 * rectangle instead, matching what LDLib2's own {@code SpriteTexture} receives.</p>
 *
 * <p>Vertices are written straight into a {@code guiTexture} buffer with normalised UVs, so neither the
 * texture-manager size probe nor {@code GuiGraphics.blit}'s integer-pixel path is involved.</p>
 */
final class SheetTexture implements IGuiTexture {

    private final ResourceLocation sheet;
    private final int sheetWidth;
    private final int sheetHeight;
    private final int u0;
    private final int v0;
    private final int u1;
    private final int v1;

    SheetTexture(ResourceLocation sheet, int sheetWidth, int sheetHeight,
                 int u0, int v0, int u1, int v1) {
        this.sheet = sheet;
        this.sheetWidth = sheetWidth;
        this.sheetHeight = sheetHeight;
        this.u0 = u0;
        this.v0 = v0;
        this.u1 = u1;
        this.v1 = v1;
    }

    @Override
    public IGuiTexture copy() {
        return new SheetTexture(sheet, sheetWidth, sheetHeight, u0, v0, u1, v1);
    }

    /** The only entry point that carries the element rectangle rather than the mouse position. */
    @Override
    public void draw(GUIContext context, float x, float y, float width, float height) {
        drawInternal(context.graphics, x, y, width, height);
    }

    @Override
    public void draw(GuiGraphics graphics, float x, float y, float width, float height,
                     float partialTick, float mouseX, float mouseY) {
        // Reached only for callers that bypass GUIContext; x/y are then already the target rectangle.
        drawInternal(graphics, x, y, width, height);
    }

    private void drawInternal(GuiGraphics graphics, float x, float y, float width, float height) {
        if (width <= 0 || height <= 0 || u1 <= u0 || v1 <= v0) return;

        float su0 = u0 / (float) sheetWidth;
        float sv0 = v0 / (float) sheetHeight;
        float su1 = u1 / (float) sheetWidth;
        float sv1 = v1 / (float) sheetHeight;

        Matrix4f pose = graphics.pose().last().pose();
        MultiBufferSource.BufferSource buffers = graphics.bufferSource();
        RenderType type = LDLibRenderTypes.guiTexture(sheet);
        VertexConsumer vertex = buffers.getBuffer(type);
        RenderSystem.disableDepthTest();

        float x1 = x + width;
        float y1 = y + height;

        vertex.addVertex(pose, x, y, 0).setColor(1f, 1f, 1f, 1f).setUv(su0, sv0);
        vertex.addVertex(pose, x, y1, 0).setColor(1f, 1f, 1f, 1f).setUv(su0, sv1);
        vertex.addVertex(pose, x1, y1, 0).setColor(1f, 1f, 1f, 1f).setUv(su1, sv1);
        vertex.addVertex(pose, x1, y, 0).setColor(1f, 1f, 1f, 1f).setUv(su1, sv0);

        buffers.endBatch(type);
        RenderSystem.enableDepthTest();
    }
}
