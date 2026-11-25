package cn.kycraft.kuayue.content.tech_tree;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaPositionType;

import java.util.Map;
import java.util.Set;

/**
 * Draws the predecessor -> successor connectors between plates.
 *
 * <p>1.0 routed these with {@code AStarPathFinding} through an occupancy grid. That is a lot of work for a
 * grid that is already column/row aligned, so every edge here is a two-segment elbow instead: out of the
 * parent's edge, along the gutter between two depths, then into the child. The 1x1 line pixel and the four
 * arrowhead sprites are the same ones 1.0 uses.</p>
 *
 * <p>Folding: an edge is drawn when at least one end is inside the scroll window, with the folded end
 * clamped to the window edge. Skipping such edges entirely made every fold look like a broken chain.</p>
 */
final class BoardConnectors {

    private BoardConnectors() {
    }

    /**
     * Rebuilds the connector layer.
     *
     * @param layer     the connector element layer, cleared by the caller
     * @param layout    BFS column/row pairs for the group
     * @param visible   node locations that may be drawn
     * @param nodes     snapshot nodes by location
     * @param scrollRow first row currently shown
     */
    static void rebuild(UIElement layer, Map<NodeLocation, int[]> layout, Set<NodeLocation> visible,
                        Map<NodeLocation, TreeSnapshot.NodeView> nodes, int scrollRow) {
        for (Map.Entry<NodeLocation, int[]> entry : layout.entrySet()) {
            NodeLocation from = entry.getKey();
            if (!visible.contains(from)) continue;
            TreeSnapshot.NodeView node = nodes.get(from);
            if (node == null) continue;

            int fromX = BoardMetrics.positionOf(layout, from)[0];
            int fromY = entry.getValue()[0] * BoardMetrics.STEP_Y - scrollRow * BoardMetrics.STEP_Y;
            boolean fromVisible = BoardMetrics.inWindow(fromY);

            for (NodeLocation to : node.next()) {
                if (!visible.contains(to)) continue;
                int[] toAt = layout.get(to);
                if (toAt == null) continue;

                int toX = BoardMetrics.positionOf(layout, to)[0];
                int toY = toAt[0] * BoardMetrics.STEP_Y - scrollRow * BoardMetrics.STEP_Y;
                boolean toVisible = BoardMetrics.inWindow(toY);

                // Both ends folded: nothing of this edge belongs on screen.
                if (!fromVisible && !toVisible) continue;

                int drawFromY = fromVisible ? fromY : BoardMetrics.clampToWindow(fromY);
                int drawToY = toVisible ? toY : BoardMetrics.clampToWindow(toY);
                connect(layer, fromX, drawFromY, toX, drawToY, toVisible);
            }
        }
    }

    /**
     * One elbow between two plates, in window-local pixels.
     *
     * <p>Both stubs are always drawn, however short. With {@code STEP_X = 40} and {@code CELL_W = 32} the
     * gutter is eight pixels wide, and skipping the short leg is what previously left a vertical run
     * hanging with no arrow on it.</p>
     */
    private static void connect(UIElement layer, int fromX, int fromY, int toX, int toY, boolean toVisible) {
        int midX = BoardMetrics.CELL_W / 2;

        // Same branch: a plain vertical run from the parent's bottom edge to the child's top edge.
        if (fromX == toX) {
            int lineX = fromX + midX;
            int top = Math.min(fromY, toY) + BoardMetrics.CELL_H;
            int bottom = Math.max(fromY, toY);
            if (bottom > top) vline(layer, lineX, top, bottom - top);
            if (!toVisible) return;
            if (toY > fromY) {
                arrow(layer, TechTreeTextures.lineDownArrow(), lineX - 4, toY - 4, 8, 4);
            } else if (toY < fromY) {
                arrow(layer, TechTreeTextures.lineUpArrow(), lineX - 4, toY + BoardMetrics.CELL_H - 3, 8, 4);
            }
            return;
        }

        // Different branch: down out of the parent, across, then in from the side of the child.
        int exitY = fromY + BoardMetrics.CELL_H;
        // Enter at the child's top edge: the gutter run comes down and stops there, with the arrow on it.
        int entryY = toY;
        int gutterY = Math.max(exitY + 1, (exitY + entryY) / 2);

        vline(layer, fromX + midX, exitY, Math.max(1, gutterY - exitY));
        int left = Math.min(fromX + midX, toX + midX);
        int right = Math.max(fromX + midX, toX + midX);
        if (right > left) hline(layer, left, gutterY, right - left);
        vline(layer, toX + midX, gutterY, Math.max(1, entryY - gutterY));

        if (toVisible) arrow(layer, TechTreeTextures.lineDownArrow(), toX + midX - 4, entryY - 4, 8, 4);
    }

    /** Horizontal run of 1x1 line pixels, one GUI pixel tall. */
    private static void hline(UIElement layer, int x, int y, int length) {
        if (length <= 0) return;
        UIElement bar = new UIElement();
        bar.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                .setPosition(YogaEdge.LEFT, (float) (x + BoardMetrics.LINE_ORIGIN))
                .setPosition(YogaEdge.TOP, (float) (y + BoardMetrics.LINE_ORIGIN))
                .setWidth(length).setHeight(1));
        bar.style(s -> s.background(TechTreeTextures.linePixel()));
        layer.addChild(bar);
    }

    /** Vertical run of 1x1 line pixels, one GUI pixel wide. */
    private static void vline(UIElement layer, int x, int y, int length) {
        if (length <= 0) return;
        UIElement bar = new UIElement();
        bar.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                .setPosition(YogaEdge.LEFT, (float) (x + BoardMetrics.LINE_ORIGIN))
                .setPosition(YogaEdge.TOP, (float) (y + BoardMetrics.LINE_ORIGIN))
                .setWidth(1).setHeight(length));
        bar.style(s -> s.background(TechTreeTextures.linePixel()));
        layer.addChild(bar);
    }

    /** One arrowhead sprite, 8x4 or 4x8; 1.0 centres these on the run's end pixel. */
    private static void arrow(UIElement layer, IGuiTexture texture, int x, int y, int width, int height) {
        UIElement head = new UIElement();
        head.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                .setPosition(YogaEdge.LEFT, (float) (x + BoardMetrics.LINE_ORIGIN))
                .setPosition(YogaEdge.TOP, (float) (y + BoardMetrics.LINE_ORIGIN))
                .setWidth(width).setHeight(height));
        head.style(s -> s.background(texture));
        layer.addChild(head);
    }
}
