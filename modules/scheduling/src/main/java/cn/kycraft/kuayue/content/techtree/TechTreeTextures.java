package cn.kycraft.kuayue.content.techtree;

import com.lowdragmc.lowdraglib2.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Visual placeholders for the blueprint table.
 *
 * <p>The NeoKuayue GUI sheets are copied into {@code assets/kuayue/textures/gui/tech_tree/} and the UV
 * rectangles below are transcribed from the reference widgets, but drawing slices of a non-atlas GUI
 * sheet through LDLib2's texture API is not resolved yet. Everything therefore renders as flat colours
 * so the layout and interactions stay usable; swapping in real slices later only touches this class.</p>
 */
final class TechTreeTextures {

    static final ResourceLocation TABLE_BG =
            ResourceLocation.fromNamespaceAndPath("kuayue", "textures/gui/tech_tree/blueprint_table/blueprint_table.png");
    static final ResourceLocation TABLE_BUTTONS =
            ResourceLocation.fromNamespaceAndPath("kuayue", "textures/gui/tech_tree/blueprint_table/blueprint_buttons.png");

    // --- flat palette ---------------------------------------------------------------------------

    static final int BOARD_BG = 0xFF2B2118;
    static final int NODE_BG = 0xFF4A3A28;
    static final int NODE_BG_DARK = 0xFF2A2118;
    static final int NODE_EPIC = 0xFF6B4E1E;
    static final int NODE_DONE = 0xFF2F4A2C;
    static final int SLOT_BG = 0x66000000;
    static final int SLOT_OK = 0x5522FF22;
    static final int SLOT_MISSING = 0x55FF2222;
    static final int ACCENT = 0xFFC9A227;

    /** Board background. */
    static IGuiTexture board() {
        return new ColorRectTexture(BOARD_BG);
    }

    /** Node plate background; {@code done} wins over the epic tint. */
    static IGuiTexture nodePlate(boolean epic, boolean done) {
        int color = done ? NODE_DONE : (epic ? NODE_EPIC : NODE_BG);
        return new ColorRectTexture(color);
    }

    static IGuiTexture nodePlateDark(boolean epic) {
        return new ColorRectTexture(NODE_BG_DARK);
    }

    static IGuiTexture slot(boolean ok) {
        return new ColorRectTexture(SLOT_BG);
    }

    static IGuiTexture slotMark(boolean ok) {
        return new ColorRectTexture(ok ? SLOT_OK : SLOT_MISSING);
    }

    static IGuiTexture accent() {
        return new ColorRectTexture(ACCENT);
    }

    static IGuiTexture progressBar() {
        return new ColorRectTexture(ACCENT);
    }

    static IGuiTexture outline(int color) {
        return new ColorBorderTexture(1, color);
    }
}
