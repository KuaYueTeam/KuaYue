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
    /** Node plate sprite: one 64x32 sheet, drawn at 32x16 on the board. */
    private static final ResourceLocation NODE_PLATE =
            ResourceLocation.fromNamespaceAndPath("kuayue",
                    "textures/gui/tech_tree/blueprint_table/node_plate.png");

    private static final ResourceLocation UNLOCK_BOARD =
            ResourceLocation.fromNamespaceAndPath("kuayue", "textures/gui/tech_tree/blueprint_table/group_unlock_board.png");

    private static final ResourceLocation TABLE_SHEET =
            ResourceLocation.fromNamespaceAndPath("kuayue", "textures/gui/tech_tree/blueprint_table/blueprint_table.png");

    private TechTreeTextures() {
    }

    /** The original 300x150 blueprint-table GUI sheet, drawn in the menu host. */
    /**
     * Arbitrary UV slice of a cover texture.
     *
     * <p>Cover JSON addresses textures as {@code kuayue:gui/tech_tree/blueprint_cover/foo}, i.e. without the
     * {@code textures/} segment and {@code .png} suffix, so the resource id is built here rather than being
     * repeated at every call site.</p>
     */
    static final class CoverSlice implements IGuiTexture {

        private final ResourceLocation texture;
        private final float u0;
        private final float v0;
        private final float u1;
        private final float v1;

        CoverSlice(ResourceLocation path, float u0, float v0, float u1, float v1) {
            this.texture = ResourceLocation.fromNamespaceAndPath(path.getNamespace(),
                    "textures/" + path.getPath() + ".png");
            this.u0 = u0;
            this.v0 = v0;
            this.u1 = u1;
            this.v1 = v1;
        }

        @Override
        public IGuiTexture copy() {
            return new CoverSlice(texture, u0, v0, u1, v1);
        }

        @Override
        public void draw(com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext context,
                         float x, float y, float width, float height) {
            // Reuse the board's own sheet painter: it already writes normalised UVs into a guiTexture buffer
            // and avoids both the texture-manager size probe and GuiGraphics.blit's integer path.
            new SheetTexture(texture, 1, 1, 0, 0, 1, 1).drawUv(context.graphics,
                    x, y, width, height, u0, v0, u1, v1);
        }

        @Override
        public void draw(net.minecraft.client.gui.GuiGraphics graphics, float x, float y,
                         float width, float height, float partialTick, float mouseX, float mouseY) {
            // Same path as {@link SheetTexture#drawInternal}: the GuiGraphics rectangle is already the target,
            // so no GUIContext is needed to recover it.
            new SheetTexture(texture, 1, 1, 0, 0, 1, 1).drawUv(graphics, x, y, width, height, u0, v0, u1, v1);
        }
    }

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

    /**
     * The board's node plate, from the project's own sprite.
     *
     * <p>Drawn at 32x16 (half the source size: an integer ratio, so the pixels stay crisp). The sprite
     * carries no state variants yet, so epic/unlocked reuse it — a later revision can add strips to the
     * sheet and slice them here.</p>
     */
    static IGuiTexture nodePlateSprite() {
        // Resolve the texture once so the failure mode is visible in the log: LDLib's guiTexture render
        // type binds whatever the TextureManager holds for this id, and an unregistered id binds nothing —
        // the quad is then drawn with no texture and the plate appears as a hole.
        var texture = net.minecraft.client.Minecraft.getInstance().getTextureManager().getTexture(NODE_PLATE, null);
        TechTreeSystem.logInfo("node_plate texture resolved: {}", texture != null);
        return new SheetTexture(NODE_PLATE, 64, 32, 0, 0, 64, 32);
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

    // --- connection lines, UVs taken from 1.0 {@code TechTreeLine} --------------------------

    /** 1x1 line pixel, tiled along a segment. */
    static IGuiTexture linePixel() {
        return slice(39, 32, 40, 33);
    }

    /** 8x4 connector stubs, as used by 1.0 for horizontal joins. */
    static IGuiTexture lineUpArrow() {
        return slice(36, 32, 44, 36);
    }

    static IGuiTexture lineDownArrow() {
        return slice(36, 44, 44, 48);
    }

    /** 4x8 connector stubs, as used by 1.0 for vertical joins. */
    static IGuiTexture lineLeftArrow() {
        return slice(32, 36, 36, 44);
    }

    static IGuiTexture lineRightArrow() {
        return slice(44, 36, 48, 44);
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
     * Confirm button, top 16x16 of the 1.0 {@code confirmBtnLight} cell.
     *
     * <p>1.0 draws that cell at 16x32 on a screen; the board is a 300x150 sheet painted on a table, and a
     * 16x32 sprite there stretched its inner art badly. Only the upper square is taken, which is the frame
     * the tick sits in, and the button is laid out square at 16x16.</p>
     */
    static IGuiTexture confirm() {
        return slice(80, 64, 96, 80);
    }

    /** Confirm button, hover frame: same square from {@code confirmBtnDark}. */
    static IGuiTexture confirmHover() {
        return slice(96, 64, 112, 80);
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
