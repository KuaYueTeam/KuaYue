package cn.kycraft.kuayue.client.creative;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Up/down arrow from KuaYue 1.0's creative vehicle selector. */
final class CreativeFilterScrollButton extends AbstractWidget {
    private static final ResourceLocation BACKGROUND =
            ResourceLocation.fromNamespaceAndPath("kuayue", "textures/gui/button_background.png");
    private static final int ARROW_SIZE = 12;
    private static final int ARROW_OFFSET = (CreativeFilterButton.SIZE - ARROW_SIZE) / 2;

    enum Direction {
        UP("up_button", "kuayue.creative_filter.scroll_up"),
        DOWN("down_button", "kuayue.creative_filter.scroll_down");

        private final ResourceLocation texture;
        private final String tooltipKey;

        Direction(String texture, String tooltipKey) {
            this.texture = ResourceLocation.fromNamespaceAndPath(
                    "kuayue",
                    "textures/gui/" + texture + ".png"
            );
            this.tooltipKey = tooltipKey;
        }
    }

    private final Runnable action;
    private final ResourceLocation texture;

    CreativeFilterScrollButton(int x, int y, Direction direction, Runnable action) {
        super(x, y, CreativeFilterButton.SIZE, CreativeFilterButton.SIZE,
                Component.translatable(direction.tooltipKey));
        this.texture = direction.texture;
        this.action = action;
        setTooltip(Tooltip.create(getMessage()));
    }

    @Override
    protected void renderWidget(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        graphics.setColor(1.0F, 1.0F, 1.0F, isActive() ? 1.0F : 0.35F);
        try {
            graphics.blit(
                    BACKGROUND,
                    getX(), getY(), 0,
                    0.0F, 0.0F,
                    width, height,
                    CreativeFilterButton.SIZE, CreativeFilterButton.SIZE
            );
            graphics.blit(
                    texture,
                    getX() + ARROW_OFFSET, getY() + ARROW_OFFSET, 0,
                    0.0F, 0.0F,
                    ARROW_SIZE, ARROW_SIZE,
                    ARROW_SIZE, ARROW_SIZE
            );
        } finally {
            graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        action.run();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
    }
}
