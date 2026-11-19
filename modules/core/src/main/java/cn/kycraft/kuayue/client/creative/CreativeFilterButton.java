package cn.kycraft.kuayue.client.creative;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Compact item-backed selector used beside an existing creative tab. */
final class CreativeFilterButton extends AbstractWidget {
    private final CreativeFilterCatalog.Entry entry;
    private final Runnable action;
    private boolean selected;

    CreativeFilterButton(
            int x,
            int y,
            CreativeFilterCatalog.Entry entry,
            boolean selected,
            Runnable action
    ) {
        super(x, y, 82, 20, Component.translatable(entry.nameKey()));
        this.entry = entry;
        this.action = action;
        this.selected = selected;
        if (entry.descriptionKey() != null) {
            setTooltip(Tooltip.create(Component.translatable(entry.descriptionKey())));
        }
    }

    void setSelected(boolean selected) {
        this.selected = selected;
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
        int background = selected ? 0xE04F7D38 : (isHovered() ? 0xE05C6673 : 0xD0353C45);
        graphics.fill(getX(), getY(), getX() + width, getY() + height, background);
        graphics.fill(getX(), getY(), getX() + 2, getY() + height, selected ? 0xFFB6F397 : 0xFF8EA4BE);
        graphics.renderItem(icon(), getX() + 3, getY() + 2);
        String text = Component.translatable(entry.nameKey()).getString();
        var font = Minecraft.getInstance().font;
        String clipped = font.plainSubstrByWidth(text, width - 31);
        graphics.drawString(font, clipped, getX() + 26, getY() + 6, 0xFFF4F7FA, false);
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
