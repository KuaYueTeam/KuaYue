package cn.kycraft.kuayue.content.techtree;

import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import cn.kycraft.kuayue.content.techtree.net.TechTreeRpc;
import lib.kasuga.rendering.models.mc.Constants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaPositionType;
import org.joml.Matrix4f;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The blueprint table GUI, painted flat on the physical table top instead of in a container screen.
 *
 * <p>This is the world-space host the module was heading for: the element tree is laid out in the same
 * 300x150 pixel space as the GUI sheet, then registered with KasugaLibNext's world-UI backend
 * ({@code Constants.UI_BACKEND}) which paints it on top of the table model and routes mouse input
 * through {@link lib.kasuga.rendering.models.mc.backend.ui.WorldUiInputRouter}.</p>
 *
 * <p>Two halves, and the split matters:</p>
 * <ul>
 *   <li>the {@link Surface} is the registered {@link Renderable}; the backend asks it for its pixel
 *       viewport ({@link LayoutElement}) and its input surface ({@link GuiEventListener}) by identity,
 *       so a lambda would lose both;</li>
 *   <li>{@link #BODY} is the live board. It is repainted in place (Yoga reads only the changed values)
 *       when the snapshot, the focused node or the checked node changes, so closing and reopening the
 *       table costs nothing and a 10-tick refresh does not rebuild every plate.</li>
 * </ul>
 *
 * <p>Right click on the table mounts the surface and opens {@code openPointer()} so the system cursor can
 * hit the plates. Escape closes it; the server is told through {@link TechTreeRpc#close()}. Nothing here
 * needs a container menu, so nothing pauses and mouse-look keeps working while the board is up.</p>
 */
final class TechTreeTableSurface implements LayoutElement, GuiEventListener, Renderable {

    /** Board size in GUI pixels, matching the blueprint sheet. */
    static final int WIDTH = 300;
    static final int HEIGHT = 150;

    /**
     * Distance at which the board stops being drawn.
     *
     * <p>Every mounted board costs a full offscreen pass per frame ({@code WorldUiCanvas}: bind a
     * framebuffer, render the element tree, blit the result as a world quad). Text at ~5 mm per pixel is
     * unreadable past a few blocks anyway, so boards beyond this range — including boards in chunks far
     * from the player — are skipped entirely. The surface stays registered, so nothing has to be remounted
     * when the player walks back.</p>
     */
    private static final double RENDER_DISTANCE = 24.0;

    /** World size of one GUI pixel; the same scale the car-model panel used. */
    private static final float PIXEL = 0.0045f * 0.875f;

    /**
     * Height above the block origin where the board plane sits.
     *
     * <p>{@code TOP_Y} (1.01) is the table top as reported by the block, but the drawn model surface is
     * above the collision box, so the plane is lifted like the blueprint scene is.</p>
     */
    private static final float SURFACE_Y = 1.175f;

    /**
     * How far the plane is pushed toward the table's far edge, in blocks (table-local Z, away from the
     * player). The sheet sits at yaw 180 relative to the table, so a positive local Z reads as "deeper".
     * 1/8 of a block, per the placement request.
     */
    private static final float SURFACE_BACK = 0.125f + 1f / 16f;

    /** How far the plane is lowered, in blocks (1/16). The sheet was floating a hair above the top. */
    private static final float SURFACE_DROP = 1f / 16f - 1f / 32f;

    /** Live boards by table position. The surface is a client-side singleton per table. */
    private static final Map<BlockPos, TechTreeTableSurface> OPEN = new HashMap<>();

    /** Input layer currently owned by this surface, so closing never steals another UI's pointer. */
    private static Object pointerScreen;

    private TechTreeTableSurface(@Nullable BlueprintTableBlockEntity table) {
        this.table = table;
    }

    @Nullable
    private BlueprintTableBlockEntity table;
    private Renderable attached;

    /** Last distance check result; the backend asks every frame, the check only runs on movement. */
    private double lastCullCheck = -1;
    private boolean visibleInRange;

    /**
     * Mounts the board on one table and keeps it there.
     *
     * <p>The board is furniture, not a modal: it is painted on the table top for as long as the block
     * exists, so a player walking past reads the plans without having to click anything. There is
     * therefore no open/close cycle here — only mount/detach driven by the block's life cycle and by
     * {@link #pruneStale()}.</p>
     *
     * <p>Mounting is idempotent: the surface is cached per position, and the same element tree is reused
     * so the Yoga layout only ever reads changed values.</p>
     */
    static void mount(BlueprintTableBlockEntity table) {
        if (Constants.UI_BACKEND == null) {
            TechTreeSystem.logWarn("World UI backend is not available; the blueprint table stays empty");
            return;
        }
        BlockPos pos = table.getBlockPos().immutable();
        TechTreeTableSurface surface = OPEN.computeIfAbsent(pos, key -> new TechTreeTableSurface(null));
        surface.table = table;
        surface.attach();
        if (browsed == null) browsed = pos;
        ensureSnapshot();
    }

    /**
     * Fetches the tree once per session.
     *
     * <p>Every table on the map paints the same tree, so one snapshot serves them all; the request is
     * only issued when nothing is cached yet (the client tick keeps it fresh afterwards).</p>
     */
    private static void ensureSnapshot() {
        if (!ClientTreeState.groups().isEmpty()) {
            return;
        }
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

    /** The table the player last interacted with; a right click only changes where clicks land. */
    @Nullable
    private static BlockPos browsed;

    /** Whether any board was in range on the previous check; drives the single approach repaint. */
    private static boolean lastVisible;

    /** True when at least one mounted board is inside {@link #RENDER_DISTANCE} of the camera. */
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

    /** True once the client knows there is anything to draw (used to skip empty mounts). */
    static boolean isMounted() {
        return !OPEN.isEmpty();
    }

    /**
     * Mounts the boards of every blueprint table around the player.
     *
     * <p>{@code setPlacedBy} only runs for a player placement; a table already in a saved chunk, or one
     * that finished loading after the join, has to be found from the level.</p>
     *
     * <p>The scan is deliberately tiny. The first version walked a 129x129x129 cube — 2.1 million
     * {@code getBlockEntity} calls per pass — which showed up as a frame-time spike even though the
     * average FPS stayed flat. It now walks only the block positions of already-mounted tables plus a
     * short list of pending positions collected from chunk loads, so the steady-state cost is
     * proportional to the number of tables, not to the size of the search volume.</p>
     */
    static void mountNearby() {
        var mc = Minecraft.getInstance();
        var level = mc.level;
        if (level == null || mc.player == null || Constants.UI_BACKEND == null) {
            PENDING_MOUNTS.clear();
            return;
        }
        // Positions reported by ChunkEvent.Load since the last pass: this is the only source of new
        // tables, so nothing else has to be searched for.
        for (BlockPos pos : PENDING_MOUNTS) {
            if (level.getBlockEntity(pos) instanceof BlueprintTableBlockEntity table) {
                mount(table);
            }
        }
        PENDING_MOUNTS.clear();

        // Tables whose block entity went away without onRemove running (chunk unload) are dropped here.
        for (BlockPos pos : new ArrayList<>(OPEN.keySet())) {
            if (!(level.getBlockEntity(pos) instanceof BlueprintTableBlockEntity)) forget(pos);
        }
    }

    /** Tables announced by a chunk load, mounted on the next client tick. */
    private static final Set<BlockPos> PENDING_MOUNTS = new LinkedHashSet<>();

    /** Records a blueprint table found while a chunk was loading; mounted on the next client tick. */
    static void announce(BlockPos pos) {
        if (pos != null) PENDING_MOUNTS.add(pos.immutable());
    }

    /**
     * Mounts tables of every chunk already loaded around the player, once.
     *
     * <p>Used on join: chunk-load events fired before the client had a player are missed, and walking the
     * level's loaded chunks (not a block cube) is the cheap way to catch up. Roughly one hash lookup per
     * loaded chunk instead of one per block.</p>
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
                // getChunkNow returns only chunks that are already in memory, which is exactly what this
                // pass wants: it never forces a load, so the join hook cannot stall the render thread.
                var chunk = mc.level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue;
                chunk.getBlockEntities().forEach((pos, entity) -> {
                    if (entity instanceof BlueprintTableBlockEntity) announce(pos);
                });
            }
        }
        mountNearby();
    }

    /**
     * Mounts the board on a table the player right-clicked.
     *
     * <p>Kept as the interaction entry point only to make that table the one whose clicks take focus:
     * the board itself has been on the table since the block loaded.</p>
     */
    static void focusTable(BlueprintTableBlockEntity table) {
        if (table == null) return;
        BlockPos pos = table.getBlockPos().immutable();
        browsed = pos;
        mount(table);
        Minecraft.getInstance().execute(TechTreeTableSurface::refresh);
        openPointer();
    }

    /**
     * Drops the board of a table that no longer exists.
     *
     * <p>{@code onRemove} is not guaranteed to run before the block entity disappears (chunk unload, a
     * datapack block change), so mounts are validated against the level each tick as well.</p>
     */
    static void pruneStale() {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            closeAll();
            return;
        }
        for (BlockPos pos : new ArrayList<>(OPEN.keySet())) {
            if (!(level.getBlockEntity(pos) instanceof BlueprintTableBlockEntity)) forget(pos);
        }
    }

    /**
     * Input layer over the boards.
     *
     * <p>Without a {@code WorldUiInputScreen} the backend routes clicks from the crosshair and the cursor
     * stays locked to mouse-look, so plates could never be hit. It is opened explicitly, and closed again
     * when the player leaves (Escape, chat, another screen).</p>
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
     * <p>{@code WorldUiInputScreen} is a vanilla screen and cannot call back here, so the check is
     * deferred one tick: by then {@code mc.screen} is either null/another screen (Escape, chat) or a
     * fresh pointer screen already replaced this one.</p>
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

    /** Unmounts every board on the client, e.g. on logout. */
    static void closeAll() {
        for (TechTreeTableSurface surface : OPEN.values()) surface.detach();
        OPEN.clear();
        browsed = null;
        releasePointer();
    }

    /** Unmounts one table. */
    static void forget(BlockPos pos) {
        if (pos == null) return;
        TechTreeTableSurface surface = OPEN.remove(pos);
        if (surface != null) surface.detach();
        if (browsed != null && browsed.equals(pos)) browsed = null;
    }

    private static void releasePointer() {
        if (pointerScreen == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == pointerScreen) mc.setScreen(null);
        pointerScreen = null;
    }

    /**
     * Repaints once when a board that was skipped comes back into view.
     *
     * <p>{@link #refresh()} is called on every snapshot and focus change; while the player was far away
     * those calls returned early, so the tree shown on approach can be stale (an unlock that happened in
     * another chunk). This restores it without repainting every tick.</p>
     */
    static void repaintOnApproach() {
        if (OPEN.isEmpty()) return;
        boolean visible = anyInRange();
        if (visible && !lastVisible) BODY.repaint();
        lastVisible = visible;
    }

    /** Repaints every mounted board from the current client state. */
    static void refresh() {
        if (OPEN.isEmpty()) return;
        BODY.repaint();
    }

    /** Re-evaluates which nodes may be drawn (unlock changed, advancement earned, group published). */
    static void invalidateVisibility() {
        recomputeVisible();
        refresh();
    }


    private void attach() {
        if (attached == null) attached = this;
        Constants.UI_BACKEND.addRenderable(attached, tableTransform(table));
        BODY.mount(WIDTH, HEIGHT);
        recomputeVisible();
        BODY.repaint();
    }

    private void detach() {
        if (attached != null && Constants.UI_BACKEND != null) Constants.UI_BACKEND.removeRenderable(attached);
    }

    /**
     * Pixel space of the board to its world plane.
     *
     * <p>Same convention as the car-model panel: local +X right, local +Y down; the XY plane is laid onto
     * the table's XZ plane and turned 180 degrees so the text reads the right way round from the player's
     * side of the table.</p>
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
                .translate(-WIDTH * 0.5f, -HEIGHT * 0.5f, 0f);
    }

    // --- Renderable / LayoutElement / GuiEventListener -----------------------------------------

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!inRange()) return;
        BODY.render(graphics, mouseX, mouseY, partialTick);
    }

    /**
     * Culls by camera distance.
     *
     * <p>Cheap, camera-space check without a frustum pass: a board planted on the far side of a wall still
     * costs its offscreen pass, but one 200 blocks away costs nothing. Re-evaluated at most every 500 ms so
     * the per-frame path stays a field read.</p>
     */
    private boolean inRange() {
        var player = Minecraft.getInstance().player;
        if (player == null || table == null) return false;
        long now = System.nanoTime();
        if (lastCullCheck >= 0 && now - (long) lastCullCheck < 500_000_000L) return visibleInRange;
        lastCullCheck = now;
        var center = table.getBlockPos().getCenter();

        // Behind the player: the panel is a 1.35x0.68 block sheet on the table top, so a plain dot product
        // on the look vector is exact enough and needs no frustum math.
        var eye = player.getEyePosition();
        var toBoard = center.subtract(eye).normalize();
        var look = player.getLookAngle();
        if (look.dot(toBoard) < BACKFACING_DOT) {
            visibleInRange = false;
            return false;
        }
        visibleInRange = eye.distanceToSqr(center) <= RENDER_DISTANCE * RENDER_DISTANCE;
        return visibleInRange;
    }

    /**
     * Look vector threshold below which the board is skipped.
     *
     * <p>{@code -0.35} keeps it drawn while it is anywhere in the peripheral view and skips only the
     * clearly-behind case, which is what a player standing at a table and turning away does most of the
     * time. The board is redrawn by {@link #repaintOnApproach()} as soon as it comes back.</p>
     */
    private static final double BACKFACING_DOT = -0.35;

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
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public void visitWidgets(java.util.function.Consumer<net.minecraft.client.gui.components.AbstractWidget> consumer) {
    }

    /** Panel-local rect: the backend uses it to place and clip the canvas. */
    @Override
    public ScreenRectangle getRectangle() {
        return new ScreenRectangle(0, 0, WIDTH, HEIGHT);
    }

    private boolean focused;

    @Override
    public void setFocused(boolean focused) {
        this.focused = focused;
        BODY.ui.getWidget().setFocused(focused);
    }

    @Override
    public boolean isFocused() {
        return focused;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        BODY.ui.getWidget().mouseMoved(mouseX, mouseY);
    }

    /**
     * Handles a click on the board.
     *
     * <p>{@code positionOn} is the panel-local point from the router's ray, so {@code hitTest} below can
     * find the element under the cursor without depending on the widget's own hover bookkeeping.</p>
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = BODY.ui.getWidget().mouseClicked(mouseX, mouseY, button);
        if (!handled) handled = manualClick(mouseX, mouseY, button);
        // listenerSize tells the two failure modes apart: 0 means the backend never registered an input
        // surface for this panel (its per-frame projection threw before addSurface), non-zero but unhandled
        // means the surface exists and the ray or the element dispatch is wrong.
        int listeners = Constants.UI_BACKEND == null ? -1 : Constants.UI_BACKEND.listenerSize();
        int renderables = Constants.UI_BACKEND == null ? -1 : Constants.UI_BACKEND.renderableSize();
        TechTreeSystem.logInfo("Board click at ({}, {}) button={} handled={} pointerOpen={} listeners={} renderables={}",
                mouseX, mouseY, button, handled, isPointerOpen(), listeners, renderables);
        return handled;
    }

    /**
     * Resolves a click against the board's own layout when the element tree does not take it.
     *
     * <p>The world-UI router only registers the whole panel as an input surface; whether a click reaches a
     * child element depends on LDLib2's widget dispatching, and an exception thrown earlier in the frame
     * skips input registration entirely. Hit-testing here keeps every button usable regardless of that: the
     * plate rects and the two detail-pane rects are known constants, and the actions are the same ones the
     * element handlers call.</p>
     *
     * @return true when the click matched something and the action was run
     */
    private static boolean manualClick(double mouseX, double mouseY, int button) {
        // Right click only: a left click on a table is "break the block", so the board must not steal it.
        if (button != 1) return false;

        // Group strip first: it sits left of the plate grid and is the only way to reach a group that has not
        // been published. The element tree cannot receive these clicks while the world-UI input surface is
        // missing, so the hit test is done here against the same geometry the strip is laid out with.
        if (mouseX >= GROUP_X && mouseX < GROUP_X + GROUP_SIZE && mouseY >= GROUP_Y) {
            int row = (int) ((mouseY - GROUP_Y) / GROUP_STEP);
            List<ResourceLocation> ids = new ArrayList<>(ClientTreeState.groups().keySet());
            if (row >= 0 && row < Math.min(ids.size(), 6)
                    && mouseY - GROUP_Y - row * GROUP_STEP < GROUP_SIZE) {
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
            boolean unlockable = ClientTreeState.lastCheck() != null
                    && ClientTreeState.lastCheck().unlockable();
            if (inRect(mouseX, mouseY, TechTreeDetailPanel.CONFIRM_HIT)
                    && (unlockable || ClientTreeState.checkedNode() == null)) {
                confirm(node);
                return true;
            }
            return false;
        }

        // Tree view: the plate rects are the same ones the layout produced.
        ResourceLocation group = ClientTreeState.selectedGroup();
        if (group == null) return false;
        Map<NodeLocation, int[]> layout = ClientTreeState.layout(group);
        for (Map.Entry<NodeLocation, int[]> entry : layout.entrySet()) {
            NodeLocation location = entry.getKey();
            if (!VISIBLE.contains(location)) continue;
            TreeSnapshot.NodeView node = ClientTreeState.nodes().get(location);
            if (node == null) continue;
            int[] at = positionOf(layout, location);
            if (!inRect(mouseX, mouseY, new int[]{
                    GRID_X + at[0] - HIT_PAD, GRID_Y + at[1] - HIT_PAD, CELL_HIT, CELL_HIT})) {
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

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return BODY.ui.getWidget().mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return BODY.ui.getWidget().mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return BODY.ui.getWidget().mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    // --- the board ------------------------------------------------------------------------------

    /**
     * Upper bound on drawn nodes per group.
     *
     * <p>The board is a fixed 288x134 pixel sheet and every plate is 20x24 with spacing, so the
     * reachable area holds roughly 5 columns by 4 rows. Drawing anything past that would put plates on
     * the table's edge; the overflow is announced in the title instead of silently clipped.</p>
     */
    private static final int MAX_PLATES = 20;

    /** Plate metrics: 20x20 icon plus the 6/4 pixel gutters used by the GUI grid. */
    /** Group strip: 18x18 buttons at x=2, stacked 20px apart, so a click target is a real target. */
    static final int GROUP_X = 2;
    static final int GROUP_Y = 20;
    static final int GROUP_SIZE = 18;
    static final int GROUP_STEP = 20;

    private static final int CELL = 20;
    /** Extra clickable margin around a plate; the sprite keeps its 20x20 size inside. */
    private static final int HIT_PAD = 3;
    private static final int CELL_HIT = CELL + HIT_PAD * 2;
    private static final int STEP_X = 26;
    private static final int STEP_Y = 24;
    private static final int COLUMNS = 5;
    private static final int GRID_X = 14;
    private static final int GRID_Y = 20;

    /** Live board DOM. Rebuilt only when the tree itself changes; repaint() re-resolves the views. */
    private static final Board BODY = new Board();

    /** Latest visibility answer, refreshed by {@link #recomputeVisible()}. */
    private static final Set<NodeLocation> VISIBLE = new LinkedHashSet<>();

    /**
     * Client-side visibility of a node.
     *
     * <p>The plate list is rebuilt from this set, not from the snapshot alone: {@code
     * ClientTreeState.visible} only knows the never-hidden rule, while the server's
     * {@code visibleNodes}/{@code visibleGroups} sets are the authority for gates that were satisfied
     * after the tree loaded. Anything the server marked visible or unlocked is drawn.</p>
     */
    private static void recomputeVisible() {
        VISIBLE.clear();
        TreeSnapshot snapshot = ClientTreeState.snapshot();
        for (TreeSnapshot.NodeView node : snapshot.nodes.values()) {
            if (ClientTreeState.visible(node.location())) VISIBLE.add(node.location());
        }
        VISIBLE.addAll(snapshot.progress.visibleNodes);
        VISIBLE.addAll(snapshot.progress.unlocked);
        // A node whose predecessor is unlocked is reachable, so it is drawn even when the server never listed
        // it: visibility is only pushed for nodes that start hidden, and revealing a successor whose own
        // hide rule is empty (the common case) would otherwise leave it off the board.
        for (TreeSnapshot.NodeView node : snapshot.nodes.values()) {
            for (NodeLocation prev : node.prev()) {
                if (snapshot.progress.unlocked.contains(prev)) {
                    VISIBLE.add(node.location());
                    break;
                }
            }
        }
    }

    /** Where one plate sits; a group's layout is its BFS column/row pair. */
    private static int[] positionOf(Map<NodeLocation, int[]> layout, NodeLocation location) {
        int[] columnRow = layout.get(location);
        if (columnRow == null) return new int[]{0, 0};
        return new int[]{Math.max(0, columnRow[0]) * STEP_X, Math.max(0, columnRow[1]) * STEP_Y};
    }

    private static boolean indexable(int[] position) {
        return position[0] / STEP_X < COLUMNS;
    }

    /**
     * The board: title, node plates and the focused node's detail pane.
     *
     * <p>Kept as one object because the element tree of a {@link ModularUI} is owned by that UI; the
     * same tree is shared by every mounted table, and a rebuild would lose the click handlers'
     * closures. All handlers read the live client state instead of closing over a snapshot, which is
     * what lets the plates survive a refresh.</p>
     *
     * <p>There is no group strip and no open/close state: the board is a permanent part of the table,
     * so it shows one group (the focused node's, else the selected one) with nothing to toggle.</p>
     */
    private static final class Board {

        private final ModularUI ui;
        private final UIElement root;
        private final UIElement plates;
        private final UIElement groupStrip;
        private final Label title;
        private final UIElement detailLayer;
        private boolean mounted;

        private Board() {
            root = new UIElement();
            root.setId("tech_tree_table_surface");
            root.layout(l -> l.setWidth(WIDTH).setHeight(HEIGHT));
            root.style(s -> s.background(TechTreeTextures.tableSheet()));

            title = new Label();
            title.textStyle(t -> t.textColor(0xFF1E2A44).adaptiveWidth(false).adaptiveHeight(true));
            title.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                    .setPosition(YogaEdge.LEFT, 6f).setPosition(YogaEdge.TOP, 3f)
                    .setWidth(WIDTH - 12).setHeight(12));
            root.addChild(title);

            groupStrip = new UIElement();
            groupStrip.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                    .setPosition(YogaEdge.LEFT, (float) GROUP_X).setPosition(YogaEdge.TOP, (float) GROUP_Y)
                    .setWidth(GROUP_SIZE).setHeight(HEIGHT - GROUP_Y - 4));
            root.addChild(groupStrip);

            plates = new UIElement();
            plates.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                    .setPosition(YogaEdge.LEFT, (float) GRID_X).setPosition(YogaEdge.TOP, (float) GRID_Y)
                    .setWidth(WIDTH - GRID_X - 4).setHeight(HEIGHT - GRID_Y - 4));
            root.addChild(plates);

            detailLayer = new UIElement();
            detailLayer.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                    .setPosition(YogaEdge.LEFT, 0f).setPosition(YogaEdge.TOP, 0f)
                    .setWidth(WIDTH).setHeight(HEIGHT));
            root.addChild(detailLayer);

            ui = ModularUI.of(UI.of(root));
        }

        /** Builds the layout tree. Yoga node access before this throws. */
        void mount(int width, int height) {
            if (mounted) return;
            ui.init(width, height);
            mounted = true;
        }

        void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            if (!mounted) mount(WIDTH, HEIGHT);
            ui.getWidget().render(graphics, mouseX, mouseY, partialTick);
            // The canvas shares the main buffer source; text batches must be submitted before the
            // framebuffer read, otherwise every glyph is dropped and only the rectangles show.
            graphics.flush();
        }

        /** Re-resolves every view against the current state and rewrites the element tree in place. */
        void repaint() {
            if (!mounted) return;
            // Nothing outside the draw distance to update; the next mount/refresh repaints on approach.
            if (OPEN.isEmpty() || !anyInRange()) return;
            recomputeVisible();

            NodeLocation focused = TechTreeScreen.getMenuFocus();
            TreeSnapshot.NodeView focusedNode = focused == null ? null : ClientTreeState.nodes().get(focused);
            ResourceLocation group = focusedNode != null ? focusedNode.location().groupId()
                    : ClientTreeState.selectedGroup();
            if (group == null && !ClientTreeState.groups().isEmpty()) {
                group = ClientTreeState.groups().keySet().iterator().next();
                ClientTreeState.selectGroup(group);
            }
            TreeSnapshot.GroupView view = group == null ? null : ClientTreeState.groups().get(group);

            title.setText(Component.translatable(view == null ? "tech_tree.board.title" : view.nameKey()));

            // The strip is what selects a group in the first place; forgetting it here left the board with
            // no way to reach the second group at all.
            rebuildGroupStrip(group);

            final ResourceLocation publishGroup = group;
            clear(detailLayer);
            if (TechTreeGroupPublishPanel.needsPublish(publishGroup)) {
                // Unpublished group: the tree is not readable yet, so the panel that publishes it takes the board.
                clear(plates);
                detailLayer.addChild(TechTreeGroupPublishPanel.build(publishGroup,
                        () -> publish(publishGroup)));
                return;
            }
            rebuildPlates(group);
            rebuildDetail(focusedNode);
        }

        private void clear(UIElement element) {
            for (UIElement child : new ArrayList<>(element.getChildren())) element.removeChild(child);
        }

        /**
         * Left column of the board: one button per group.
         *
         * <p>Groups are not all published: {@code overhead_line_blueprint_group} has
         * {@code initial_visibility = false} and only becomes reachable through its unlock condition. The
         * strip is therefore the way to select any group, and selecting an unpublished one opens
         * {@link TechTreeGroupPublishPanel} instead of the tree.</p>
         */
        private void rebuildGroupStrip(@Nullable ResourceLocation selected) {
            clear(groupStrip);
            int index = 0;
            for (ResourceLocation id : ClientTreeState.groups().keySet()) {
                if (index++ >= 6) break;
                TreeSnapshot.GroupView view = ClientTreeState.groups().get(id);
                final int row = index - 1;
                final boolean published = !TechTreeGroupPublishPanel.needsPublish(id);
                UIElement button = new UIElement();
                button.setId("group_" + id);
                button.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                        .setPosition(YogaEdge.LEFT, 0f)
                        .setPosition(YogaEdge.TOP, (float) (row * GROUP_STEP))
                        .setWidth(GROUP_SIZE).setHeight(GROUP_SIZE));
                button.style(s -> s.background(published
                                ? TechTreeTextures.groupButton()
                                : TechTreeTextures.nodePlateDark(false))
                        .overlay(view.icon().isEmpty() ? null : new ItemStackTexture(view.icon())));
                button.addEventListener(UIEvents.CLICK, event -> {
                    ClientTreeState.selectGroup(id);
                    repaint();
                    event.stopPropagation();
                });
                groupStrip.addChild(button);
            }
        }

        private void rebuildPlates(@Nullable ResourceLocation group) {
            clear(plates);
            if (group == null) return;
            Map<NodeLocation, int[]> layout = ClientTreeState.layout(group);
            List<Map.Entry<NodeLocation, int[]>> drawn = new ArrayList<>();
            for (Map.Entry<NodeLocation, int[]> entry : layout.entrySet()) {
                if (!VISIBLE.contains(entry.getKey())) continue;
                if (!indexable(positionOf(layout, entry.getKey()))) continue;
                drawn.add(entry);
            }
            drawn.sort((a, b) -> {
                int byRow = Integer.compare(a.getValue()[1], b.getValue()[1]);
                return byRow != 0 ? byRow : Integer.compare(a.getValue()[0], b.getValue()[0]);
            });
            int limit = Math.min(drawn.size(), MAX_PLATES);
            for (int i = 0; i < limit; i++) {
                NodeLocation location = drawn.get(i).getKey();
                TreeSnapshot.NodeView node = ClientTreeState.nodes().get(location);
                if (node == null) continue;
                int[] at = positionOf(layout, location);
                plates.addChild(plate(node, at[0], at[1]));
            }
            if (drawn.size() > MAX_PLATES) {
                Label more = new Label();
                more.setText(Component.translatable("tech_tree.board.more", drawn.size() - MAX_PLATES));
                more.textStyle(t -> t.textColor(0xFF7A3B16).adaptiveHeight(true));
                more.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                        .setPosition(YogaEdge.LEFT, 2f)
                        .setPosition(YogaEdge.TOP, (float) (HEIGHT - GRID_Y - 16))
                        .setWidth(160).setHeight(9));
                plates.addChild(more);
            }
        }

        private UIElement plate(TreeSnapshot.NodeView node, int x, int y) {
        boolean unlocked = ClientTreeState.unlocked(node.location());
        boolean epic = node.type() == NodeType.EPIC;
        boolean focused = node.location().equals(TechTreeScreen.getMenuFocus());

        // Outer element is the click target: it is padded by HIT_PAD on every side, so the tiny 20x20
        // plate gets a 26x26 hit box without the sprite growing.
        UIElement element = new UIElement();
        element.setId("node_" + node.location());
        element.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                .setPosition(YogaEdge.LEFT, (float) (x - HIT_PAD))
                .setPosition(YogaEdge.TOP, (float) (y - HIT_PAD))
                .setWidth(CELL_HIT).setHeight(CELL_HIT));
        element.addEventListener(UIEvents.CLICK, event -> {
            focus(node);
            event.stopPropagation();
        });

        ItemStack logo = node.logo();
        UIElement sprite = new UIElement();
        sprite.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                .setPosition(YogaEdge.LEFT, (float) HIT_PAD)
                .setPosition(YogaEdge.TOP, (float) HIT_PAD)
                .setWidth(CELL).setHeight(CELL));
        sprite.style(s -> s.background(unlocked
                        ? TechTreeTextures.nodePlate(epic, true)
                        : TechTreeTextures.nodePlateDark(epic))
                .overlay(logo.isEmpty() ? null : new ItemStackTexture(logo)));
        element.addChild(sprite);

        // State dot, as in 1.0 {@code TechTreeLabel}: every node carries one — white once unlocked, red while
        // a predecessor is still missing. The first version drew the white dot only for unlocked nodes, so a
        // locked node whose predecessors were all done showed no dot at all; 1.0 marks that case white too.
        boolean required = !unlocked && hasMissingPredecessor(node);
        UIElement dot = new UIElement();
        dot.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                .setPosition(YogaEdge.LEFT, (float) (HIT_PAD + CELL - 7))
                .setPosition(YogaEdge.TOP, (float) (HIT_PAD + CELL - 7))
                .setWidth(7).setHeight(7));
        dot.style(s -> s.background(TechTreeTextures.stateDot(required)));
        element.addChild(dot);
        return element;
    }

    /**
     * True when any predecessor of the node is still locked.
     *
     * <p>Read straight from the snapshot's edge list, so the dot is correct before the player clicks
     * anything; 1.0 computed the same flag when it built its labels.</p>
     */
    private static boolean hasMissingPredecessor(TreeSnapshot.NodeView node) {
        for (NodeLocation prev : node.prev()) {
            if (!ClientTreeState.unlocked(prev)) return true;
        }
        return false;
    }

        private void rebuildDetail(@Nullable TreeSnapshot.NodeView node) {
            clear(detailLayer);
            if (node == null) return;
            detailLayer.addChild(TechTreeDetailPanel.build(node, ClientTreeState.unlocked(node.location())));
        }
    }

    // --- actions --------------------------------------------------------------------------------

    /**
     * Focuses a node, or returns to the tree when it is already focused.
     *
     * <p>The pre-check request is what colours the slots and fills the progress arrow, so it is issued
     * here rather than on the plate: one round trip per focus change instead of one per repaint.</p>
     */
    static void focus(TreeSnapshot.NodeView node) {
        if (node.location().equals(TechTreeScreen.getMenuFocus())) {
            backToTree();
            return;
        }
        TechTreeScreen.setMenuFocus(node.location());
        TechTreeTableSurface.BODY.repaint();
        TechTreeRpc.canUnlock().request(node.location().toString()).thenAccept(result -> {
            ClientTreeState.applyCheck(node.location(), result);
            Minecraft.getInstance().execute(TechTreeTableSurface::refresh);
        });
    }

    /** Leaves the detail pane; the board keeps its group. */
    static void backToTree() {
        TechTreeScreen.clearMenuFocus();
        TechTreeTableSurface.BODY.repaint();
    }

    /**
     * Confirms the focused node.
     *
     * <p>The answer is the authoritative snapshot, so the board is repainted from it directly instead of
     * waiting for the throttled client refresh.</p>
     */
    static void confirm(TreeSnapshot.NodeView node) {
        TechTreeRpc.unlockNode().request(node.location().toString()).whenComplete((snapshot, error) -> {
            if (error != null) {
                TechTreeSystem.logWarn("Tech tree unlock failed for " + node.location(), error);
                return;
            }
            Minecraft.getInstance().execute(() -> {
                ClientTreeState.apply(snapshot);
                ClientTreeState.applyCheck(node.location(), CheckResult.empty());
                invalidateVisibility();
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
                invalidateVisibility();
            });
        });
    }
}
