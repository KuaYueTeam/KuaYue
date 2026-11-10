package cn.kycraft.kuayue.content.techtree;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

/**
 * Paints the {@link TechTreeTableMenu} slots onto the physical blueprint table.
 *
 * <p>The world board is composited as one flat plane inside an offscreen canvas, so a live {@code ItemSlot}
 * cannot live there: its drag state is bound to the gameplay menu and the player has no screen open while
 * looking at the table. The slots are therefore ordinary container slots of the table menu, drawn here at
 * the place the model's table top is, using the same local-to-world matrix the board uses.</p>
 */
final class TableSlotProjection {

    private TableSlotProjection() {
    }

    /**
     * Draws slot plates and the items inside them along the project's own convention: the board is
     * {@link TechTreeBoard#WIDTH}x{@link TechTreeBoard#HEIGHT} pixels mapped onto the table top by
     * {@link TechTreeBoard#tableTransform}.
     */
    static void render(BlueprintTableBlockEntity table, TechTreeTableMenu menu, GuiGraphics gui, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (table == null || menu == null || mc.player == null) return;

        Matrix4f local = TechTreeBoard.tableTransform(table, RenderSystem.getModelViewMatrix());
        gui.flush();
        var pose = gui.pose();
        pose.pushPose();
        try {
            pose.last().pose().set(local);
            for (TechTreeTableMenu.ProjectedSlot slot : TechTreeTableMenu.projected(menu)) {
                ItemStack stack = slot.getItem();
                int x = slot.boardX();
                int y = slot.boardY();
                int size = slot.boardSize();
                gui.fill(x, y, x + size, y + size, 0xAA2A2018);
                gui.fill(x, y, x + size, y + 1, 0xFFC9A227);
                gui.fill(x, y + size - 1, x + size, y + size, 0xFFC9A227);
                gui.fill(x, y, x + 1, y + size, 0xFFC9A227);
                gui.fill(x + size - 1, y, x + size, y + size, 0xFFC9A227);
                if (!stack.isEmpty()) {
                    gui.renderItem(stack, x + 7, y + 7);
                    gui.renderItemDecorations(mc.font, stack, x + 7, y + 7);
                }
            }
        } finally {
            pose.popPose();
        }
        gui.flush();
    }
}
