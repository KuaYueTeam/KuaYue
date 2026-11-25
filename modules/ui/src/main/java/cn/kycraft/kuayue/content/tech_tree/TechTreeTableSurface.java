package cn.kycraft.kuayue.content.tech_tree;

import cn.kycraft.kuayue.content.tech_tree.net.TechTreeRpc;
import lib.kasuga.rendering.models.mc.Constants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * The blueprint table's world-space host.
 *
 * <p>Owns the lifecycle (mount, cull, input layer, RPC) and the {@link BoardView} that draws the tree.
 * Geometry lives in {@link BoardMetrics}, a single plate in {@link BoardPlates}, an edge in
 * {@link BoardConnectors} — this class is the wiring between them and the world-UI backend.</p>
 *
 * <p>The surface is the registered {@link Renderable}; the backend asks it for its pixel viewport
 * ({@link LayoutElement}) and its input surface ({@link GuiEventListener}) by identity, so a lambda would
 * lose both.</p>
 */
final class TechTreeTableSurface implements LayoutElement, GuiEventListener, Renderable {

    /**
     * Distance at which the board stops being drawn.
     *
     * <p>Every mounted board costs a full offscreen pass per frame. Text at ~5 mm per pixel is unreadable
     * past a few blocks anyway, so distant boards are skipped entirely; the surface stays registered, so
     * nothing has to be remounted when the player walks back.</p>
     */
    private static final double RENDER_DISTANCE = 24.0;

    /** Look vector threshold below which the board is skipped (clearly behind the player). */
    private static final double BACKFACING_DOT = -0.35;

    /** World size of one GUI pixel; the same scale the car-model panel used. */
    private static final float PIXEL = 0.0045f * 0.875f;

    /**
     * Height above the block origin where the board plane sits.
     *
     * <p>The drawn model surface is above the collision box, so the plane is lifted like the blueprint
     * scene is.</p>
     */
    private static final float SURFACE_Y = 1.175f;

    /** How far the plane is pushed toward the table's far edge, in blocks (table-local Z). */
    private static final float SURFACE_BACK = 0.125f + 1f / 16f;

    /** How far the plane is lowered, in blocks (1/32). */
    private static final float SURFACE_DROP = 1f / 16f - 1f / 32f;

    /** Live boards by table position. The surface is a client-side singleton per table. */
    private static final Map<BlockPos, TechTreeTableSurface> OPEN = new HashMap<>();

    /** Tables announced by a chunk load, mounted on the next client tick. */
    private static final Set<BlockPos> PENDING_MOUNTS = new LinkedHashSet<>();

    /** Input layer currently owned by this surface, so closing never steals another UI's pointer. */
    private static Object pointerScreen;

    /** The table the player last interacted with; a right click only changes where clicks land. */
    @Nullable
    private static BlockPos browsed;

    /** Whether any board was in range on the previous check; drives the single approach repaint. */
    private static boolean lastVisible;

    /** First row currently shown, and the row counts that bound it. */
    private static int scrollRow;
    private static int rowsTotal;
    private static int rowsVisible;

    /** Cover page state: shown until the entry button is clicked. */
    private static boolean coverVisible;

    /** Positions whose cover page has already been shown. */
    private static final Set<BlockPos> coverShown = new java.util.HashSet<>();

    /** The board element tree, shared by every mounted table. */
    private static final BoardView BODY = new BoardView();

    @Nullable
    private BlueprintTableBlockEntity table;
    private Renderable attached;

    /** Last distance check result; the backend asks every frame, the check only runs on movement. */
    private double lastCullCheck = -1;
    private boolean visibleInRange;

    private TechTreeTableSurface(@Nullable BlueprintTableBlockEntity table) {
        this.table = table;
    }

    // --- lifecycle -----------------------------------------------------------------------------

    /**
     * Mounts the board on one table and keeps it there.
     *
     * <p>The board is furniture, not a modal: it is painted on the table top for as long as the block
     * exists. Mounting is idempotent, and the same element tree is reused so Yoga only reads changed
     * values.</p>
     */
    static void mount(BlueprintTableBlockEntity table) {
        if (Constants.UI_BACKEND == null) {
            TechTreeSystem.logWarn("World UI backend is not available; the blueprint table stays empty");
            return;
        }
        BlockPos pos = table.getBlockPos().immutable();
        if (browsed == null) browsed = pos;

        // Cover state must be decided before attach(): attach() paints the board once, and whatever that
        // repaint sees is the first frame. Deciding afterwards showed the tree and never repainted.
        boolean fresh = coverShown.add(pos);
        if (fresh && pos.equals(browsed) && BlueprintCoverManager.get().defaultCover() != null) {
            coverVisible = true;
            TechTreeSystem.logInfo("Board {} opens on its cover page", pos);
        }

        TechTreeTableSurface surface = OPEN.computeIfAbsent(pos, key -> new TechTreeTableSurface(null));
        surface.table = table;
        surface.attach();
        ensureSnapshot();
    }

    /** Fetches the tree once per session; every table paints the same tree. */
    private static void ensureSnapshot() {
        if (!ClientTreeState.groups().isEmpty()) return;
        var player = Minecraft.getInstance().player;
        TechTreeRpc.open().run(new TechTreeRpc.OpenPayload(player != null && player.isCreative()))
                .whenComplete((snapshot, error) -> {
                    if (error != null) {
                        TechTreeSystem.logWarn("Tech tree snapshot request failed", error);
                        return;
                    }
                    Minecraft.getInstance().execute(() -> {
                        ClientTreeState.apply(snapshot);
                        invalidateVisibility();
                    });
                });
    }

    /** True when at least one mounted board is inside the draw distance. */
    private static boolean anyInRange() {
        for (TechTreeTableSurface surface : OPEN.values()) {
            if (surface.inRange()) return true;
        }
        return false;
    }

    /** True when a board is mounted and close enough to be drawn; the cache refresh keys off this. */
    static boolean hasVisibleBoard() {
        return !OPEN.isEmpty() && anyInRange();
    }

    /** True once the client has anything mounted. */
    static boolean isMounted() {
        return !OPEN.isEmpty();
    }

    /**
     * Mounts tables of every chunk already loaded around the player, once.
     *
     * <p>Used on join: chunk-load events fired before the client had a player are missed, and walking the
     * level's loaded chunks (not a block cube) is the cheap way to catch up.</p>
     */
    static void mountLoadedChunks() {
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || Constants.UI_BACKEND == null) return;
        var origin = mc.player.blockPosition();
        int chunkRadius = 8;
        int centerX = origin.getX() >> 4;
        int centerZ = origin.getZ() >> 4;
        for (int cx = centerX - chunkRadius; cx <= centerX + chunkRadius; cx++) {
            for (int cz = centerZ - chunkRadius; cz <= centerZ + chunkRadius; cz++) {
                // getChunkNow only returns chunks already in memory: the join hook must not force a load.
                var chunk = mc.level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue;
                chunk.getBlockEntities().forEach((pos, entity) -> {
                    if (entity instanceof BlueprintTableBlockEntity) announce(pos);
                });
            }
        }
        drainPendingMounts();
    }

    /** Records a blueprint table found while a chunk was loading; mounted on the next client tick. */
    static void announce(BlockPos pos) {
        if (pos != null) PENDING_MOUNTS.add(pos.immutable());
    }

    /**
     * Mounts boards announced since the last pass and drops mounts whose table is gone.
     *
     * <p>Replaces the 129x129x129 block scan the first version used (2.1 million {@code getBlockEntity}
     * calls per pass). The steady-state cost is now proportional to the number of tables.</p>
     */
    static void mountNearby() {
        var mc = Minecraft.getInstance();
        var level = mc.level;
        if (level == null || mc.player == null || Constants.UI_BACKEND == null) {
            PENDING_MOUNTS.clear();
            return;
        }
        drainPendingMounts();
        for (BlockPos pos : new ArrayList<>(OPEN.keySet())) {
            if (!(level.getBlockEntity(pos) instanceof BlueprintTableBlockEntity)) forget(pos);
        }
    }

    private static void drainPendingMounts() {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            PENDING_MOUNTS.clear();
            return;
        }
        for (BlockPos pos : PENDING_MOUNTS) {
            if (level.getBlockEntity(pos) instanceof BlueprintTableBlockEntity table) mount(table);
        }
        PENDING_MOUNTS.clear();
    }

    /** Mounts the board on a table the player right-clicked. */
    static void focusTable(BlueprintTableBlockEntity table) {
        if (table == null) return;
        BlockPos pos = table.getBlockPos().immutable();
        browsed = pos;
        if (!coverShown.contains(pos)) coverVisible = true;
        mount(table);
        Minecraft.getInstance().execute(TechTreeTableSurface::refresh);
        openPointer();
    }

    /**
     * Unmounts a table immediately, without waiting for {@link #pruneStale()}.
     *
     * <p>{@code Block.onRemove} is not guaranteed to run before the block entity disappears, and the
     * throttled prune is seconds away, so breaking a table used to leave its board hanging in the air.</p>
     */
    static void pruneNow(BlockPos pos) {
        if (pos == null) return;
        forget(pos.immutable());
        refresh();
    }

    /**
     * Drops boards whose table no longer exists.
     *
     * <p>{@code onRemove} cannot be relied on (chunk unload, datapack changes), so mounts are validated
     * against the level on a slow tick as well — the table under the player first.</p>
     */
    static void pruneStale() {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            closeAll();
            return;
        }
        if (browsed != null && !(level.getBlockEntity(browsed) instanceof BlueprintTableBlockEntity)) {
            pruneNow(browsed);
        }
        for (BlockPos pos : new ArrayList<>(OPEN.keySet())) {
            if (!(level.getBlockEntity(pos) instanceof BlueprintTableBlockEntity)) forget(pos);
        }
    }

    /** Unmounts one table. */
    static void forget(BlockPos pos) {
        if (pos == null) return;
        TechTreeTableSurface surface = OPEN.remove(pos);
        if (surface != null) surface.detach();
        if (browsed != null && browsed.equals(pos)) browsed = null;
        coverShown.remove(pos);
    }

    /** Unmounts every board on the client, e.g. on logout. */
    static void closeAll() {
        for (TechTreeTableSurface surface : OPEN.values()) surface.detach();
        OPEN.clear();
        browsed = null;
        coverShown.clear();
        releasePointer();
    }

    private void attach() {
        if (attached == null) attached = this;
        Constants.UI_BACKEND.addRenderable(attached, tableTransform(table));
        BODY.mountTree(BoardMetrics.WIDTH, BoardMetrics.HEIGHT);
        BODY.repaint(scrollRow, rowsTotal, rowsVisible);
    }

    private void detach() {
        if (attached != null && Constants.UI_BACKEND != null) Constants.UI_BACKEND.removeRenderable(attached);
    }

    // --- input layer ---------------------------------------------------------------------------

    /**
     * Installs the invisible input layer; without it the cursor stays locked to mouse-look.
     *
     * <p>Opened explicitly, and closed again when the player leaves (Escape, chat, another screen).</p>
     */
    static void openPointer() {
        if (Constants.UI_BACKEND == null || OPEN.isEmpty()) return;
        if (Minecraft.getInstance().screen != null) return;
        try {
            pointerScreen = Constants.UI_BACKEND.openPointer();
            TechTreeSystem.logInfo("Board input layer opened");
        } catch (IllegalStateException alreadyOpenOrNoWorld) {
            pointerScreen = null;
            TechTreeSystem.logWarn("Board input layer NOT opened: {}", alreadyOpenOrNoWorld.getMessage());
        }
    }

    /**
     * Closes the input layer once its screen goes away.
     *
     * <p>{@code WorldUiInputScreen} cannot call back here, so the check is deferred one tick.</p>
     */
    static void schedulePointerClose() {
        if (pointerScreen == null) return;
        Minecraft.getInstance().execute(() -> {
            if (pointerScreen != null && Minecraft.getInstance().screen != pointerScreen) {
                pointerScreen = null;
                TechTreeRpc.close().send();
            }
        });
    }

    /** True while the player is holding the cursor over the boards. */
    static boolean isPointerOpen() {
        return pointerScreen != null && Minecraft.getInstance().screen == pointerScreen;
    }

    private static void releasePointer() {
        if (pointerScreen == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == pointerScreen) mc.setScreen(null);
        pointerScreen = null;
    }

    // --- repaint -------------------------------------------------------------------------------

    /**
     * Repaints once when a board that was skipped comes back into view.
     *
     * <p>{@link #refresh()} returns early while the player is far away, so the tree shown on approach can be
     * stale.</p>
     */
    static void repaintOnApproach() {
        if (OPEN.isEmpty()) return;
        boolean visible = anyInRange();
        if (visible && !lastVisible) BODY.repaint(scrollRow, rowsTotal, rowsVisible);
        lastVisible = visible;
    }

    /** Repaints every mounted board from the current client state. */
    static void refresh() {
        if (OPEN.isEmpty()) return;
        if (coverVisible) {
            BlueprintCover cover = BlueprintCoverManager.get().defaultCover();
            if (cover != null) {
                BODY.showCover(cover);
                return;
            }
            coverVisible = false;
        }
        BODY.repaint(scrollRow, rowsTotal, rowsVisible);
    }

    /** Leaves the cover page and shows the tree; the cover's entry button is the only caller. */
    static void enterTree() {
        if (!coverVisible) return;
        BODY.leaveCover();
        coverVisible = false;
        refresh();
    }

    /** Re-evaluates which nodes may be drawn (unlock changed, advancement earned, group published). */
    static void invalidateVisibility() {
        refresh();
    }

    // --- world placement -----------------------------------------------------------------------

    /**
     * Pixel space of the board to its world plane.
     *
     * <p>Local +X right, local +Y down; the XY plane is laid onto the table's XZ plane and turned 180
     * degrees so text reads the right way round from the player's side.</p>
     */
    static Matrix4f tableTransform(BlueprintTableBlockEntity table) {
        BlockPos pos = table.getBlockPos();
        double yaw = Math.toRadians(table.yaw());
        // The shift is applied in the table's own frame, so it stays "toward the far edge" at any facing.
        float dx = (float) (Math.sin(yaw) * SURFACE_BACK);
        float dz = (float) (-Math.cos(yaw) * SURFACE_BACK);
        return new Matrix4f()
                .translate(pos.getX() + 0.5f + dx, pos.getY() + SURFACE_Y - SURFACE_DROP, pos.getZ() + 0.5f + dz)
                .rotateY((float) yaw)
                .rotateY((float) Math.toRadians(180.0f))
                .rotateX((float) Math.toRadians(90.0f))
                .scale(PIXEL, PIXEL, PIXEL)
                .translate(-BoardMetrics.WIDTH * 0.5f, -BoardMetrics.HEIGHT * 0.5f, 0f);
    }

    // --- Renderable / LayoutElement / GuiEventListener ------------------------------------------

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!inRange()) return;
        BODY.render(graphics, mouseX, mouseY, partialTick);
    }

    /**
     * Culls by camera distance.
     *
     * <p>Cheap camera-space check without a frustum pass: a board on the far side of a wall still costs its
     * offscreen pass, but one 200 blocks away costs nothing. Re-evaluated at most every 500 ms.</p>
     */
    private boolean inRange() {
        var player = Minecraft.getInstance().player;
        if (player == null || table == null) return false;
        long now = System.nanoTime();
        if (lastCullCheck >= 0 && now - (long) lastCullCheck < 500_000_000L) return visibleInRange;
        lastCullCheck = now;
        var center = table.getBlockPos().getCenter();
        var eye = player.getEyePosition();
        var toBoard = center.subtract(eye).normalize();
        if (player.getLookAngle().dot(toBoard) < BACKFACING_DOT) {
            visibleInRange = false;
            return false;
        }
        visibleInRange = eye.distanceToSqr(center) <= RENDER_DISTANCE * RENDER_DISTANCE;
        return visibleInRange;
    }

    @Override
    public int getX() {
        return 0;
    }

    @Override
    public int getY() {
        return 0;
    }

    @Override
    public void setX(int x) {
    }

    @Override
    public void setY(int y) {
    }

    @Override
    public int getWidth() {
        return BoardMetrics.WIDTH;
    }

    @Override
    public int getHeight() {
        return BoardMetrics.HEIGHT;
    }

    @Override
    public void visitWidgets(java.util.function.Consumer<net.minecraft.client.gui.components.AbstractWidget> consumer) {
    }

    @Override
    public ScreenRectangle getRectangle() {
        return new ScreenRectangle(0, 0, BoardMetrics.WIDTH, BoardMetrics.HEIGHT);
    }

    private boolean focused;

    @Override
    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    @Override
    public boolean isFocused() {
        return focused;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = manualClick(mouseX, mouseY, button);
        int listeners = Constants.UI_BACKEND == null ? -1 : Constants.UI_BACKEND.listenerSize();
        TechTreeSystem.logInfo("Board click at ({}, {}) button={} handled={} pointerOpen={} listeners={}",
                mouseX, mouseY, button, handled, isPointerOpen(), listeners);
        return handled;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return scrollY != 0 && manualScroll(mouseX, mouseY, scrollY);
    }

    // --- hit testing ---------------------------------------------------------------------------

    private static boolean manualClick(double mouseX, double mouseY, int button) {
        // Right click only: a left click on a table is "break the block".
        if (button != 1) return false;

        // The cover owns the board until its entry button is clicked.
        if (coverVisible) {
            if (BlueprintCoverPanel.enterContains(BlueprintCoverManager.get().defaultCover(), mouseX, mouseY)) {
                enterTree();
            }
            return true;
        }

        // Group strip first: it sits left of the plate field and is the only way to reach an unpublished
        // group. The element tree cannot receive these clicks while the world-UI input surface is missing.
        if (mouseX >= BoardMetrics.GROUP_X && mouseX < BoardMetrics.GROUP_X + BoardMetrics.GROUP_SIZE
                && mouseY >= BoardMetrics.GROUP_Y) {
            int row = (int) ((mouseY - BoardMetrics.GROUP_Y) / BoardMetrics.GROUP_STEP);
            ArrayList<ResourceLocation> ids = new ArrayList<>(ClientTreeState.groups().keySet());
            if (row >= 0 && row < Math.min(ids.size(), 6)
                    && mouseY - BoardMetrics.GROUP_Y - row * BoardMetrics.GROUP_STEP < BoardMetrics.GROUP_SIZE) {
                ClientTreeState.selectGroup(ids.get(row));
                refresh();
                return true;
            }
        }

        ResourceLocation selected = ClientTreeState.selectedGroup();
        if (TechTreeGroupPublishPanel.needsPublish(selected)
                && inRect(mouseX, mouseY, TechTreeGroupPublishPanel.CONFIRM_HIT)) {
            publish(selected);
            return true;
        }

        NodeLocation focused = TechTreeScreen.getMenuFocus();
        if (focused != null) {
            TreeSnapshot.NodeView node = ClientTreeState.nodes().get(focused);
            if (node == null) {
                TechTreeScreen.clearMenuFocus();
                return false;
            }
            if (inRect(mouseX, mouseY, TechTreeDetailPanel.BACK_HIT)) {
                backToTree();
                return true;
            }
            boolean unlockable = ClientTreeState.lastCheck() != null && ClientTreeState.lastCheck().unlockable();
            if (inRect(mouseX, mouseY, TechTreeDetailPanel.CONFIRM_HIT)
                    && (unlockable || ClientTreeState.checkedNode() == null)) {
                confirm(node);
                return true;
            }
            return false;
        }

        if (clickRail(mouseX, mouseY)) return true;

        if (selected == null) return false;
        Map<NodeLocation, int[]> layout = ClientTreeState.layout(selected);
        for (Map.Entry<NodeLocation, int[]> entry : layout.entrySet()) {
            NodeLocation location = entry.getKey();
            TreeSnapshot.NodeView node = ClientTreeState.nodes().get(location);
            if (node == null) continue;
            int[] at = BoardMetrics.positionOf(layout, location);
            int screenY = at[1] - scrollRow * BoardMetrics.STEP_Y;
            if (!BoardMetrics.inWindow(screenY)) continue;
            if (!inRect(mouseX, mouseY, new int[]{
                    BoardMetrics.GRID_X + at[0] - BoardMetrics.HIT_PAD,
                    BoardMetrics.GRID_Y + screenY - BoardMetrics.HIT_PAD,
                    BoardMetrics.CELL_HIT_W, BoardMetrics.CELL_HIT_H})) {
                continue;
            }
            focus(node);
            return true;
        }
        return false;
    }

    private static boolean inRect(double x, double y, int[] rect) {
        return x >= rect[0] && y >= rect[1] && x < rect[0] + rect[2] && y < rect[1] + rect[3];
    }

    // --- scrolling -----------------------------------------------------------------------------

    /**
     * Moves the plate window by whole rows.
     *
     * <p>Rows, not pixels: plates sit on a STEP_Y grid, so a pixel scroll would clip half a plate at the
     * window edge.</p>
     */
    static boolean scrollBy(int rows) {
        int max = Math.max(0, rowsTotal - rowsVisible);
        int next = Math.max(0, Math.min(max, scrollRow + rows));
        if (next == scrollRow) return false;
        scrollRow = next;
        refresh();
        return true;
    }

    /** Rows the window can still move. */
    static int scrollRange() {
        return Math.max(0, rowsTotal - rowsVisible);
    }

    /** Rail hit test: click above/below the thumb pages, on the thumb jumps proportionally. */
    static boolean clickRail(double mouseX, double mouseY) {
        if (rowsTotal <= rowsVisible) return false;
        if (mouseX < BoardMetrics.SCROLL_X || mouseX >= BoardMetrics.SCROLL_X + BoardMetrics.SCROLL_W) return false;
        if (mouseY < BoardMetrics.GRID_Y || mouseY >= BoardMetrics.GRID_Y + BoardMetrics.WINDOW_H) return false;

        int thumbH = Math.max(BoardMetrics.SCROLL_MIN_THUMB,
                BoardMetrics.WINDOW_H * rowsVisible / Math.max(1, rowsTotal));
        int travel = BoardMetrics.WINDOW_H - thumbH;
        int thumbY = travel * scrollRow / Math.max(1, rowsTotal - rowsVisible);
        double local = mouseY - BoardMetrics.GRID_Y;

        if (local >= thumbY && local <= thumbY + thumbH) {
            int target = travel == 0 ? 0
                    : (int) Math.round((local - thumbH / 2.0) / travel * (rowsTotal - rowsVisible));
            scrollRow = Math.max(0, Math.min(rowsTotal - rowsVisible, target));
        } else {
            int page = Math.max(1, rowsVisible - 1);
            scrollRow = Math.max(0, Math.min(rowsTotal - rowsVisible,
                    scrollRow + (local < thumbY ? -page : page)));
        }
        refresh();
        return true;
    }

    /** Wheel scrolling over the plate window; one notch is one row. */
    static boolean manualScroll(double mouseX, double mouseY, double delta) {
        if (TechTreeScreen.getMenuFocus() != null) return false;
        if (rowsTotal <= rowsVisible) return false;
        if (mouseX < BoardMetrics.GRID_X || mouseX >= BoardMetrics.WIDTH - 4) return false;
        if (mouseY < BoardMetrics.GRID_Y || mouseY >= BoardMetrics.GRID_Y + BoardMetrics.WINDOW_H) return false;
        return scrollBy(delta > 0 ? -1 : 1);
    }

    /**
     * Recomputes the row counts the window depends on, then repaints.
     *
     * <p>Called by the board after every change that can affect how many rows a group needs.</p>
     */
    static void noteRowCounts(int total, int shown) {
        rowsTotal = total;
        rowsVisible = Math.max(1, shown);
        scrollRow = Math.max(0, Math.min(scrollRow, Math.max(0, rowsTotal - rowsVisible)));
    }

    // --- actions -------------------------------------------------------------------------------

    /**
     * Focuses a node, or returns to the tree when it is already focused.
     *
     * <p>The pre-check request colours the slots and fills the progress arrow, so it is issued here rather
     * than on the plate: one round trip per focus change.</p>
     */
    static void focus(TreeSnapshot.NodeView node) {
        if (node.location().equals(TechTreeScreen.getMenuFocus())) {
            backToTree();
            return;
        }
        TechTreeScreen.setMenuFocus(node.location());
        refresh();
        TechTreeRpc.canUnlock().request(node.location().toString()).thenAccept(result -> {
            ClientTreeState.applyCheck(node.location(), result);
            Minecraft.getInstance().execute(TechTreeTableSurface::refresh);
        });
    }

    /** Leaves the detail pane; the board keeps its group. */
    static void backToTree() {
        TechTreeScreen.clearMenuFocus();
        refresh();
    }

    /** Confirms the focused node; the answer is the authoritative snapshot. */
    static void confirm(TreeSnapshot.NodeView node) {
        TechTreeRpc.unlockNode().request(node.location().toString()).whenComplete((snapshot, error) -> {
            if (error != null) {
                TechTreeSystem.logWarn("Tech tree unlock failed for " + node.location(), error);
                return;
            }
            Minecraft.getInstance().execute(() -> {
                ClientTreeState.apply(snapshot);
                ClientTreeState.applyCheck(node.location(), CheckResult.empty());
                refresh();
            });
        });
    }

    /** Publishes the selected group (the tree's one group-level action). */
    static void publish(@Nullable ResourceLocation group) {
        if (group == null) return;
        TechTreeRpc.unlockGroup().request(group.toString()).whenComplete((snapshot, error) -> {
            if (error != null) {
                TechTreeSystem.logWarn("Tech tree group publish failed for " + group, error);
                return;
            }
            Minecraft.getInstance().execute(() -> {
                ClientTreeState.apply(snapshot);
                refresh();
            });
        });
    }
}
