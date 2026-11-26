package cn.kycraft.kuayue.content.tech_tree;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaPositionType;

import javax.annotation.Nullable;

/**
 * The blueprint cover page, laid out exactly as 1.0 {@code BlueprintCoverPanel} reads it.
 *
 * <p>Three optional image layers plus optional text, each placed in the cover's own 242x128 pixel space.
 * The cover is centred on the board; layers use {@code horizontal_centered} / {@code vertical_centered} to
 * measure their offset from the text direction rather than from the left/top edge.</p>
 *
 * <p>Drawn as a panel rather than as a screen: on the world-space board the cover is what the table shows
 * before a group is selected, and it is replaced in place by the tree.</p>
 */
final class BlueprintCoverPanel {

    /** Cover space size, from the 1.0 sheet. */
    static final int WIDTH = 242;
    static final int HEIGHT = 128;

    private BlueprintCoverPanel() {
    }

    /**
     * Builds the cover page.
     *
     * <p>The JSON's {@code button_background} is art only — 1.0 hit-tested it from {@code BlueprintCoverPanel}
     * itself. Here the click is delivered by {@link #enterRect(int, int, int, int)} so the board's manual hit
     * test can drive it without the world-UI input layer.</p>
     *
     * @param onEnter run when the cover's entry button is clicked
     */
    static UIElement build(BlueprintCover cover, Runnable onEnter) {
        UIElement page = new UIElement();
        page.setId("tech_tree_cover");
        page.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                .setPosition(YogaEdge.LEFT, (float) originX())
                .setPosition(YogaEdge.TOP, (float) originY())
                .setWidth(WIDTH).setHeight(HEIGHT));

        if (cover.background() != null) {
            page.addChild(layer(cover.background(), 0, 0));
        }
        if (cover.title() != null) {
            page.addChild(layer(cover.title(), 0, 0));
        }
        if (cover.button() != null) {
            page.addChild(layer(cover.button(), 0, 0));
            // Invisible click target over the art: the element tree only sees clicks when the world-UI input
            // layer is registered, and the board handles that case itself.
            UIElement hit = new UIElement();
            hit.setId("tech_tree_cover_enter");
            int[] r = buttonRect(cover.button());
            hit.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                    .setPosition(YogaEdge.LEFT, (float) r[0])
                    .setPosition(YogaEdge.TOP, (float) r[1])
                    .setWidth(r[2]).setHeight(r[3]));
            hit.addEventListener(com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents.CLICK, event -> {
                onEnter.run();
                event.stopPropagation();
            });
            page.addChild(hit);
        }
        if (cover.descriptionKey() != null) {
            Label text = new Label();
            text.setText(net.minecraft.network.chat.Component.translatable(cover.descriptionKey()));
            text.textStyle(t -> t.textColor(0xFF2A2018).adaptiveWidth(false).adaptiveHeight(true)
                    .textWrap(com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap.WRAP));
            text.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                    .setPosition(YogaEdge.LEFT, 12f)
                    .setPosition(YogaEdge.TOP, 50f)
                    .setWidth(WIDTH - 24).setHeight(HEIGHT - 60));
            page.addChild(text);
        }
        return page;
    }

    /** Placement of a layer inside the cover, in cover-local pixels (same rules as {@link #layer}). */
    static int[] rect(BlueprintCover.CoverImage image) {
        int width = image.width() > 0 ? image.width() : WIDTH;
        int height = image.height() > 0 ? image.height() : HEIGHT;
        int x = image.centeredX() ? WIDTH - image.x() - width : image.x();
        int y = image.centeredY() ? HEIGHT - image.y() - height : image.y();
        if (image.positionIsCenter()) {
            x -= width / 2;
            y -= height / 2;
        }
        return new int[]{x, y, width, height};
    }

    /** Board-space rect of the entry button; used by the element target and by the manual hit test. */
    static int[] buttonRect(@Nullable BlueprintCover.CoverImage image) {
        if (image == null) return new int[]{-1, -1, 0, 0};
        int[] r = rect(image);
        return new int[]{originX() + r[0], originY() + r[1], r[2], r[3]};
    }

    /** True when a board-space point is on the cover's entry button. */
    static boolean enterContains(@Nullable BlueprintCover cover, double mouseX, double mouseY) {
        if (cover == null || cover.button() == null) return false;
        int[] r = buttonRect(cover.button());
        return mouseX >= r[0] && mouseY >= r[1] && mouseX < r[0] + r[2] && mouseY < r[1] + r[3];
    }

    /** Cover origin: centred on the tree grid, so the cover sits where the tree will appear. */
    static int originX() {
        return BoardMetrics.GRID_X
                + (BoardMetrics.WIDTH - BoardMetrics.GRID_X - WIDTH) / 2;
    }

    static int originY() {
        return (BoardMetrics.HEIGHT - HEIGHT) / 2;
    }

    /**
     * One image layer.
     *
     * <p>{@code centeredX/Y} in the JSON means "this coordinate counts from the far side", so the stored
     * value is subtracted from the cover size. {@code positionIsCenter} means the coordinate is the layer's
     * centre rather than its top-left corner.</p>
     */
    private static UIElement layer(BlueprintCover.CoverImage image, int extraX, int extraY) {
        int width = image.width() > 0 ? image.width() : WIDTH;
        int height = image.height() > 0 ? image.height() : HEIGHT;
        int x = image.centeredX() ? WIDTH - image.x() - width : image.x();
        int y = image.centeredY() ? HEIGHT - image.y() - height : image.y();
        if (image.positionIsCenter()) {
            x -= width / 2;
            y -= height / 2;
        }
        final int fx = x + extraX;
        final int fy = y + extraY;

        UIElement element = new UIElement();
        element.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                .setPosition(YogaEdge.LEFT, (float) fx)
                .setPosition(YogaEdge.TOP, (float) fy)
                .setWidth(width).setHeight(height));
        element.style(s -> s.background(new TechTreeTextures.CoverSlice(
                image.location(), image.u0(), image.v0(), image.u1(), image.v1())));
        return element;
    }
}
