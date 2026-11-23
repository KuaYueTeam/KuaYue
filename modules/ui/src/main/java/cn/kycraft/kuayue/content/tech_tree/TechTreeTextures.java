package cn.kycraft.kuayue.content.tech_tree;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Textures of the blueprint table board.
 *
 * <p>Small widgets (node plates, buttons, bars) still come from the NeoKuayue GUI sheets; the large
 * 300x150 table background is gone on purpose. The board is painted straight onto the physical table
 * model, so the furniture itself is the backdrop and only the tree UI is drawn on top of it.</p>
 */
final class TechTreeTextures {

    static final ResourceLocation TABLE_BUTTONS =
            ResourceLocation.fromNamespaceAndPath("kuayue", "textures/gui/tech_tree/blueprint_table/blueprint_buttons.png");

    /** Sheet dimensions, hard-coded so no texture-manager probing is involved. */
    private static final int BUTTONS = 128;

    /** Group-unlock panel: its own 120x80 sheet. */
    private static final ResourceLocation UNLOCK_BOARD =
            ResourceLocation.fromNamespaceAndPath("kuayue", "textures/gui/tech_tree/blueprint_table/group_unlock_board.png");

    private static final ResourceLocation TABLE_SHEET =
            ResourceLocation.fromNamespaceAndPath("kuayue", "textures/gui/tech_tree/blueprint_table/blueprint_table.png");

    private TechTreeTextures() {
    }

    /** The original 300x150 blueprint-table GUI sheet, drawn in the menu host. */
    static IGuiTexture tableSheet() {
        return new SheetTexture(TABLE_SHEET, 300, 150, 0, 0, 300, 150);
    }

    /** Board backdrop: warm parchment over the model, no sheet blit. */
    static IGuiTexture boardPaper() {
        return new ColorRectTexture(0xF2E8D6);
    }

    /** Blueprint-table sheet background, kept for the screen host. */
    static IGuiTexture board() {
        return boardPaper();
    }

    private static IGuiTexture slice(int x1, int y1, int x2, int y2) {
        return new SheetTexture(TABLE_BUTTONS, BUTTONS, BUTTONS, x1, y1, x2, y2);
    }

    /** Small node plate: plain / epic (gold). */
    static IGuiTexture nodePlate(boolean epic, boolean done) {
        return epic ? slice(96, 16, 116, 36) : slice(0, 16, 20, 36);
    }

    static IGuiTexture nodePlateDark(boolean epic) {
        return epic ? slice(96, 36, 116, 56) : slice(0, 36, 20, 56);
    }

    /** Group button plate: the sheet's own node plate, so no flat colour block is drawn. */
    static IGuiTexture groupButton() {
        return nodePlate(false, true);
    }

    /**
     * 7x7 state dot from the 1.0 label sheet: white when the node is unlocked, red when a predecessor is
     * still missing (1.0 {@code TechTreeLabel.finishDotMask}, {@code (25,25)-(32,32)}).
     */
    static IGuiTexture stateDot(boolean required) {
        return slice(25, 25, 32, 32);
    }

    /** 120x80 group-unlock panel sheet, as shipped by 1.0 ({@code group_unlock_board.png}). */
    static IGuiTexture unlockGroupBoard() {
        return new SheetTexture(UNLOCK_BOARD, 120, 80, 0, 0, 120, 80);
    }

    /** Completion stamp drawn behind the finished description block. */
    static IGuiTexture completedStamp() {
        return slice(0, 96, 32, 128);
    }

    /** Scroll rail: translucent track and a light thumb, drawn as flat colours (the 1.0 sheet has no rail sprite). */
    static IGuiTexture scrollTrack() {
        return new ColorRectTexture(0x33FFFFFF);
    }

    static IGuiTexture scrollThumb() {
        return new ColorRectTexture(0xCC5A6B8C);
    }

    /** Experience bar: satisfied / unsatisfied. */
    static IGuiTexture expBar(boolean enough) {
        return enough ? slice(48, 96, 57, 107) : slice(48, 112, 57, 123);
    }

    /**
     * Confirm button, 16x32 as in 1.0 {@code UnlockGroupBoard.confirmBtnLight}.
     *
     * <p>The whole rect is the button: cropping it to the top 16 rows showed only its plain upper fill, and
     * slicing a 16x16 square out of the 32x32 completion stamp at {@code (0,96)} showed the top-left corner
     * of a tick. There is no separate glyph to overlay.</p>
     */
    static IGuiTexture confirm() {
        return slice(80, 64, 96, 96);
    }

    /** Confirm button, hover frame ({@code confirmBtnDark}). */
    static IGuiTexture confirmHover() {
        return slice(96, 64, 112, 96);
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
