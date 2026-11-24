package cn.kycraft.kuayue.client.creative;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** KuaYue 1.0-style item icon selector that expands to the left when selected. */
final class CreativeFilterButton extends AbstractWidget {
    static final int SIZE = 20;
    private static final int EXPANDED_WIDTH = 40;
    private static final ResourceLocation BACKGROUND =
            ResourceLocation.fromNamespaceAndPath("kuayue", "textures/gui/button_background.png");
    private static final ResourceLocation SELECTED_BACKGROUND =
            ResourceLocation.fromNamespaceAndPath("kuayue", "textures/gui/chosen_button_background.png");

    private final CreativeFilterCatalog.Entry entry;
    private final Runnable action;
    private boolean selected;

    CreativeFilterButton(
            int x,
            int y,
            CreativeFilterCatalog.Entry entry,
            Runnable action
    ) {
        super(x, y, SIZE, SIZE, Component.translatable(entry.nameKey()));
        this.entry = entry;
        this.action = action;
        String description = entry.descriptionKey();
        setTooltip(Tooltip.create(description == null
                ? Component.translatable(entry.nameKey())
                : Component.translatable(description)));
    }

    void setSelected(boolean selected) {
        this.selected = selected;
        this.width = selected ? EXPANDED_WIDTH : SIZE;
    }

    void setVisible(boolean visible) {
        this.visible = visible;
    }

    ResourceLocation entryTab() {
        return entry.tab();
    }

    boolean matches(CreativeFilterCatalog.Entry candidate) {
        return entry.equals(candidate);
    }

    @Override
    protected void renderWidget(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        if (selected) {
            graphics.blit(
                    SELECTED_BACKGROUND,
                    getX(), getY(), 0,
                    0.0F, 0.0F,
                    EXPANDED_WIDTH, SIZE,
                    EXPANDED_WIDTH, SIZE
            );
        } else {
            graphics.blit(
                    BACKGROUND,
                    getX(), getY(), 0,
                    0.0F, 0.0F,
                    SIZE, SIZE,
                    SIZE, SIZE
            );
        }

        int iconOffset = selected ? 1 : 2;
        graphics.renderItem(icon(), getX() + iconOffset, getY() + 2);
        if (selected) {
            graphics.drawString(
                    Minecraft.getInstance().font,
                    getMessage(),
                    getX() + 21,
                    getY() + 5,
                    0xFF4699FF,
                    false
            );
        }
    }

    private ItemStack icon() {
        ResourceLocation icon = entry.iconId();
        if (icon != null && BuiltInRegistries.ITEM.containsKey(icon)) {
            Item item = BuiltInRegistries.ITEM.get(icon);
            if (item != Items.AIR) return new ItemStack(item);
        }
        return new ItemStack(Items.MINECART);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        action.run();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
        output.add(
                NarratedElementType.HINT,
                Component.translatable(selected
                        ? "kuayue.creative_filter.selected"
                        : "kuayue.creative_filter.select")
        );
    }
}
