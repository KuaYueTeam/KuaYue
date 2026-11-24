package cn.kycraft.kuayue.content.tech_tree;

import java.util.Map;

/**
 * Geometry of the blueprint board, in GUI pixels.
 *
 * <p>Split out of {@link TechTreeTableSurface} because every layout number used to live inline in a
 * 1400-line class: changing the plate size meant hunting a dozen references, and a missed one silently
 * misaligned the hit test against the drawing. Everything positional now lives here and nowhere else.</p>
 *
 * <p>The board is a fixed 300x150 sheet. Content is laid out on a cell grid inside the drawing field, and
 * the field is the scroll window: rows above it are folded, rows below are reached with the rail.</p>
 */
final class BoardMetrics {

    private BoardMetrics() {
    }

    // --- sheet ---------------------------------------------------------------------------------

    /** Board size in GUI pixels, matching the blueprint sheet. */
    static final int WIDTH = 300;
    static final int HEIGHT = 150;

    // --- plates --------------------------------------------------------------------------------

    /** Plate size: half the 64x32 node sprite, an integer ratio so the pixels stay crisp. */
    static final int CELL_W = 32;
    static final int CELL_H = 16;

    /** Extra clickable margin around a plate; the sprite keeps its own size inside. */
    static final int HIT_PAD = 3;

    static final int CELL_HIT_W = CELL_W + HIT_PAD * 2;
    static final int CELL_HIT_H = CELL_H + HIT_PAD * 2;

    /** Painted size of a plate's item icon, relative to the plate height. */
    static final float ICON_SCALE = 0.75f;

    /** Upper bound on drawn plates; the board cannot usefully show more than this. */
    static final int MAX_PLATES = 20;

    // --- grid ----------------------------------------------------------------------------------

    static final int GRID_X = 26;
    static final int GRID_Y = 20;

    /** Horizontal step between neighbouring branches, and vertical step between depths. */
    static final int STEP_X = 40;
    static final int STEP_Y = 22;

    /** How many plates fit across the field before the layout would run off the sheet. */
    static final int COLUMNS = 5;

    /**
     * Top edge of the detail pane's slot row, in board pixels.
     *
     * <p>{@link TechTreeDetailPanel} lays its consumption slots at y=105; this is the lowest row the plate
     * window may reach so the two never overlap.</p>
     */
    static final int SLOT_ROW_Y = 105;

    /** Visible plate window inside the grid area: everything above the slot row. */
    static final int WINDOW_H = SLOT_ROW_Y - GRID_Y - 2;

    // --- group strip ---------------------------------------------------------------------------

    /** Left column of group buttons; square and stacked. */
    static final int GROUP_X = 2;
    static final int GROUP_Y = 20;
    static final int GROUP_SIZE = 18;
    static final int GROUP_STEP = 20;

    // --- scroll rail ---------------------------------------------------------------------------

    static final int SCROLL_X = WIDTH - 14;
    static final int SCROLL_W = 4;
    static final int SCROLL_MIN_THUMB = 12;

    /** Overflow margin around the connector layer; arrowheads are 4px wide. */
    static final int LINE_ORIGIN = 4;

    // --- positions -----------------------------------------------------------------------------

    /**
     * Where one plate sits, in window-local pixels.
     *
     * <p>The layout pair is {@code (column, row)}: column is the BFS depth from the root, row is the order
     * inside that depth. Vertically the depth runs downward and the row spreads sideways, so a tree reads
     * top-to-bottom with its branches fanning out.</p>
     */
    static int[] positionOf(Map<NodeLocation, int[]> layout, NodeLocation location) {
        int[] columnRow = layout.get(location);
        if (columnRow == null) return new int[]{0, 0};
        return new int[]{Math.max(0, columnRow[1]) * STEP_X, Math.max(0, columnRow[0]) * STEP_Y};
    }

    /**
     * True when a plate's x fits the field.
     *
     * <p>Vertically the depth is unbounded — the rail scrolls it. Horizontally the row spread is what can
     * run off the sheet.</p>
     */
    static boolean indexable(int[] position) {
        return position[0] / STEP_X < COLUMNS;
    }

    /** True when a plate at this window-local y is fully inside the scroll window. */
    static boolean inWindow(int y) {
        return y >= 0 && y + CELL_H <= WINDOW_H;
    }

    /** Snaps a folded plate to the near window edge so its connector stops at the cut. */
    static int clampToWindow(int y) {
        return y + CELL_H <= 0 ? 0 : WINDOW_H - CELL_H;
    }
}
