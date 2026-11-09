package cn.kycraft.kuayue.content.techtree;

import com.lowdragmc.lowdraglib2.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Textures of the blueprint table, sliced from the NeoKuayue GUI sheets.
 *
 * <p>The sheets are copied verbatim into {@code assets/kuayue/textures/gui/tech_tree/} and the UV
 * rectangles match the reference widgets. {@link SheetTexture} draws the slices itself, because LDLib2's
 * {@code SpriteTexture} resolves the sheet size through the texture manager and yields a 1x1 placeholder
 * for non-atlas GUI sheets.</p>
 */
final class TechTreeTextures {

    static final ResourceLocation TABLE_BG =
            ResourceLocation.fromNamespaceAndPath("kuayue", "textures/gui/tech_tree/blueprint_table/blueprint_table.png");
    static final ResourceLocation TABLE_BUTTONS =
            ResourceLocation.fromNamespaceAndPath("kuayue", "textures/gui/tech_tree/blueprint_table/blueprint_buttons.png");

    /** Sheet dimensions, hard-coded so no texture-manager probing is involved. */
    private static final int TABLE_W = 300, TABLE_H = 150;
    private static final int BUTTONS = 128;

    // --- whole sheets ---------------------------------------------------------------------------

    /** Board background (300x150). */
    static IGuiTexture board() {
        return new SheetTexture(TABLE_BG, TABLE_W, TABLE_H, 0, 0, TABLE_W, TABLE_H);
    }

    // --- blueprint_buttons.png slices (128x128 sheet) ---------------------------------------------

    private static IGuiTexture slice(int x1, int y1, int x2, int y2) {
        return new SheetTexture(TABLE_BUTTONS, BUTTONS, BUTTONS, x1, y1, x2, y2);
    }

    /** Small node plate: plain / epic (gold), and their locked (dark) variants. */
    static IGuiTexture nodePlate(boolean epic, boolean done) {
        if (done) {
            // Finished nodes use the highlighted small plate in the reference screen.
            return epic ? slice(96, 16, 116, 36) : slice(0, 16, 20, 36);
        }
        return epic ? slice(96, 16, 116, 36) : slice(0, 16, 20, 36);
    }

    static IGuiTexture nodePlateDark(boolean epic) {
        return epic ? slice(96, 36, 116, 56) : slice(0, 36, 20, 56);
    }

    /** Completion stamp drawn behind the finished description block. */
    static IGuiTexture completedStamp() {
        return slice(0, 96, 32, 128);
    }

    /** Experience bar: satisfied / unsatisfied. */
    static IGuiTexture expBar(boolean enough) {
        return enough ? slice(48, 96, 57, 107) : slice(48, 112, 57, 123);
    }

    /** Confirm button, light state. */
    static IGuiTexture confirm() {
        return slice(48, 64, 64, 80);
    }

    /** Back arrow used by the detail pane. */
    static IGuiTexture backArrow() {
        return slice(32, 32, 48, 40);
    }

    /** Main progress arrow, cropped to {@code 48,48 - 48+53*p,64}. */
    static IGuiTexture progressArrow(float percentage) {
        float clamped = Math.max(0f, Math.min(1f, percentage));
        int width = Math.round(53f * clamped);
        if (width <= 0) return IGuiTexture.EMPTY;
        return slice(48, 48, 48 + width, 64);
    }

    // --- flat helpers still used for slots -------------------------------------------------------

    static final int SLOT_BG = 0x66000000;
    static final int SLOT_OK = 0x5522FF22;
    static final int SLOT_MISSING = 0x55FF2222;
    static final int ACCENT = 0xFFC9A227;

    /** Backdrop of the player inventory strip under the board. */
    static IGuiTexture inventoryBg() {
        return new ColorRectTexture(0xFF3A2E22);
    }

    static IGuiTexture slot() {
        return new ColorRectTexture(SLOT_BG);
    }

    static IGuiTexture slotMark(boolean ok) {
        return new ColorRectTexture(ok ? SLOT_OK : SLOT_MISSING);
    }

    static IGuiTexture accent() {
        return new ColorRectTexture(ACCENT);
    }

    static IGuiTexture outline(int color) {
        return new ColorBorderTexture(1, color);
    }
}
