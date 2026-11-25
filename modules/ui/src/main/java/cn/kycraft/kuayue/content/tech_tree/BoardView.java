package cn.kycraft.kuayue.content.tech_tree;

import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaPositionType;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The board element tree: title, group strip, connector layer, plate layer, detail layer.
 *
 * <p>Split out of {@link TechTreeTableSurface} so the drawing pass and the host lifecycle stop sharing a
 * class. The host owns mounting, input and RPC; this class owns the {@link ModularUI} and knows how to
 * repaint itself in place from the current client state.</p>
 */
final class BoardView {

    private final ModularUI ui;
    private final UIElement root;
    private final UIElement lines;
    private final UIElement plates;
    private final UIElement groupStrip;
    private final Label title;
    private final UIElement detailLayer;
    private boolean mounted;

    /** True while the cover page owns the board; the tree must not paint over it. */
    private boolean coverActive;

    /** Node locations currently drawable; recomputed on every repaint. */
    private final Set<NodeLocation> visible = new LinkedHashSet<>();

    BoardView() {
        root = new UIElement();
        root.setId("tech_tree_table_surface");
        root.layout(l -> l.setWidth(BoardMetrics.WIDTH).setHeight(BoardMetrics.HEIGHT));
        root.style(s -> s.background(TechTreeTextures.tableSheet()));

        title = new Label();
        // Ink, not black: at the board's pixel scale a near-black collapsed into a bar.
        title.textStyle(t -> t.textColor(0xFF3E4E6B).adaptiveWidth(false).adaptiveHeight(true));
        title.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                .setPosition(YogaEdge.LEFT, (float) (BoardMetrics.GRID_X + 2)).setPosition(YogaEdge.TOP, 3f)
                .setWidth(BoardMetrics.WIDTH - BoardMetrics.GRID_X - 6).setHeight(12));
        root.addChild(title);

        groupStrip = new UIElement();
        groupStrip.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                .setPosition(YogaEdge.LEFT, (float) BoardMetrics.GROUP_X)
                .setPosition(YogaEdge.TOP, (float) BoardMetrics.GROUP_Y)
                .setWidth(BoardMetrics.GROUP_SIZE)
                .setHeight(BoardMetrics.HEIGHT - BoardMetrics.GROUP_Y - 4));
        root.addChild(groupStrip);

        // Connectors sit under the plates, with a little slack so 4px arrowheads are not clipped.
        lines = new UIElement();
        lines.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                .setPosition(YogaEdge.LEFT, (float) (BoardMetrics.GRID_X - BoardMetrics.LINE_ORIGIN))
                .setPosition(YogaEdge.TOP, (float) (BoardMetrics.GRID_Y - BoardMetrics.LINE_ORIGIN))
                .setWidth(BoardMetrics.WIDTH - BoardMetrics.GRID_X - 4 + BoardMetrics.LINE_ORIGIN * 2)
                .setHeight(BoardMetrics.HEIGHT - BoardMetrics.GRID_Y - 4 + BoardMetrics.LINE_ORIGIN * 2));
        root.addChild(lines);

        plates = new UIElement();
        plates.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                .setPosition(YogaEdge.LEFT, (float) BoardMetrics.GRID_X)
                .setPosition(YogaEdge.TOP, (float) BoardMetrics.GRID_Y)
                .setWidth(BoardMetrics.WIDTH - BoardMetrics.GRID_X - 4)
                .setHeight(BoardMetrics.HEIGHT - BoardMetrics.GRID_Y - 4));
        root.addChild(plates);

        detailLayer = new UIElement();
        detailLayer.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                .setPosition(YogaEdge.LEFT, 0f).setPosition(YogaEdge.TOP, 0f)
                .setWidth(BoardMetrics.WIDTH).setHeight(BoardMetrics.HEIGHT));
        root.addChild(detailLayer);

        ui = ModularUI.of(UI.of(root));
    }

    /** Builds the layout tree. Yoga node access before this throws. */
    void mountTree(int width, int height) {
        if (mounted) return;
        ui.init(width, height);
        mounted = true;
    }

    boolean isMounted() {
        return mounted;
    }

    void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!mounted) mountTree(BoardMetrics.WIDTH, BoardMetrics.HEIGHT);
        ui.getWidget().render(graphics, mouseX, mouseY, partialTick);
        // The canvas shares the main buffer source; text batches must be submitted before the framebuffer
        // read, otherwise every glyph is dropped and only the rectangles show.
        graphics.flush();
    }

    /**
     * Re-resolves every view against the client state and rewrites the element tree in place.
     *
     * @param scrollRow first row of the plate window
     * @param rowsTotal total rows the current group needs
     * @param rowsVisible rows that fit in the window
     */
    void repaint(int scrollRow, int rowsTotal, int rowsVisible) {
        if (!mounted) return;
        recomputeVisible();

        NodeLocation focused = TechTreeScreen.getMenuFocus();
        TreeSnapshot.NodeView focusedNode = focused == null ? null : ClientTreeState.nodes().get(focused);
        ResourceLocation group = focusedNode != null ? focusedNode.location().groupId()
                : ClientTreeState.selectedGroup();
        if (group == null && !ClientTreeState.groups().isEmpty()) {
            group = ClientTreeState.groups().keySet().iterator().next();
            ClientTreeState.selectGroup(group);
        }

        // Copy before the lambda: `group` is reassigned above, so it is not effectively final.
        final ResourceLocation publishGroup = group;
        TreeSnapshot.GroupView view = group == null ? null : ClientTreeState.groups().get(group);
        title.setText(Component.translatable(view == null ? "tech_tree.board.title" : view.nameKey()));
        rebuildGroupStrip(group);
        clear(detailLayer);

        if (group != null && TechTreeGroupPublishPanel.needsPublish(group)) {
            clear(plates);
            clear(lines);
            detailLayer.addChild(TechTreeGroupPublishPanel.build(publishGroup, () -> TechTreeTableSurface.publish(publishGroup)));
            return;
        }

        if (group == null) {
            clear(plates);
            clear(lines);
            return;
        }

        rebuildPlates(group, scrollRow, rowsTotal, rowsVisible);
        rebuildDetail(focusedNode);
    }

    /** Paints a cover page instead of the tree, and returns the entry-button action to the caller. */
    void showCover(BlueprintCover cover) {
        // The cover is the only thing on the board until its entry button is clicked: the strip, the title
        // and the connector layer would otherwise show through or around the cover sheet.
        clear(plates);
        clear(lines);
        clear(groupStrip);
        clear(detailLayer);
        coverActive = true;
        detailLayer.addChild(BlueprintCoverPanel.build(cover, TechTreeTableSurface::enterTree));
    }

    /** Returns the board to the tree; the cover's entry button is the only caller. */
    void leaveCover() {
        if (!coverActive) return;
        coverActive = false;
    }

    // --- pieces --------------------------------------------------------------------------------

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
                    .setPosition(YogaEdge.TOP, (float) (row * BoardMetrics.GROUP_STEP))
                    .setWidth(BoardMetrics.GROUP_SIZE).setHeight(BoardMetrics.GROUP_SIZE));
            button.style(s -> s.background(published
                            ? TechTreeTextures.groupButton()
                            : TechTreeTextures.nodePlateDark(false))
                    .overlay(view.icon().isEmpty() ? null : new ItemStackTexture(view.icon())));
            button.addEventListener(UIEvents.CLICK, event -> {
                ClientTreeState.selectGroup(id);
                TechTreeTableSurface.refresh();
                event.stopPropagation();
            });
            groupStrip.addChild(button);
        }
    }

    private void rebuildPlates(ResourceLocation group, int scrollRow, int rowsTotal, int rowsVisible) {
        clear(plates);
        clear(lines);
        Map<NodeLocation, int[]> layout = ClientTreeState.layout(group);

        List<Map.Entry<NodeLocation, int[]>> drawn = new ArrayList<>();
        for (Map.Entry<NodeLocation, int[]> entry : layout.entrySet()) {
            if (!visible.contains(entry.getKey())) continue;
            if (!BoardMetrics.indexable(BoardMetrics.positionOf(layout, entry.getKey()))) continue;
            drawn.add(entry);
        }
        drawn.sort((a, b) -> {
            int byDepth = Integer.compare(a.getValue()[0], b.getValue()[0]);
            return byDepth != 0 ? byDepth : Integer.compare(a.getValue()[1], b.getValue()[1]);
        });

        // Rows are a property of this layout, so they are computed here rather than passed in: the host
        // never filled the counts, and a zero-height window made the depth filter below skip every plate.
        int depthTotal = 0;
        for (Map.Entry<NodeLocation, int[]> entry : drawn) {
            depthTotal = Math.max(depthTotal, entry.getValue()[0] + 1);
        }
        int depthVisible = Math.max(1, BoardMetrics.WINDOW_H / BoardMetrics.STEP_Y);
        TechTreeTableSurface.noteRowCounts(depthTotal, depthVisible);
        BoardConnectors.rebuild(lines, layout, visible, ClientTreeState.nodes(), scrollRow);

        int lastRow = scrollRow + depthVisible;
        // No cap on the visible set: the window already bounds what is drawn (depths below it are folded
        // away), so a second cap would silently drop the deepest branches of a wide group.
        for (int i = 0; i < drawn.size(); i++) {
            NodeLocation location = drawn.get(i).getKey();
            TreeSnapshot.NodeView node = ClientTreeState.nodes().get(location);
            if (node == null) continue;
            int[] at = BoardMetrics.positionOf(layout, location);
            int depthRow = drawn.get(i).getValue()[0];
            // Depths above the window are folded away; depths below stay folded until the rail moves.
            if (depthRow < scrollRow || depthRow >= lastRow) continue;
            plates.addChild(BoardPlates.build(node, at[0], at[1] - scrollRow * BoardMetrics.STEP_Y,
                    () -> TechTreeTableSurface.focus(node)));
        }

        if (rowsTotal > rowsVisible) {
            plates.addChild(scrollRail(scrollRow, rowsTotal, rowsVisible));
        }
    }

    /** Vertical rail: track plus a thumb whose height is the visible fraction. */
    private UIElement scrollRail(int scrollRow, int rowsTotal, int rowsVisible) {
        UIElement rail = new UIElement();
        rail.setId("tech_tree_scroll");
        rail.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                .setPosition(YogaEdge.LEFT, (float) (BoardMetrics.SCROLL_X - BoardMetrics.GRID_X))
                .setPosition(YogaEdge.TOP, 0f)
                .setWidth(BoardMetrics.SCROLL_W).setHeight(BoardMetrics.WINDOW_H));
        rail.style(s -> s.background(TechTreeTextures.scrollTrack()));

        int thumbH = Math.max(BoardMetrics.SCROLL_MIN_THUMB,
                BoardMetrics.WINDOW_H * rowsVisible / Math.max(1, rowsTotal));
        int travel = BoardMetrics.WINDOW_H - thumbH;
        int thumbY = rowsTotal > rowsVisible
                ? travel * scrollRow / (rowsTotal - rowsVisible) : 0;

        UIElement thumb = new UIElement();
        thumb.setId("tech_tree_scroll_thumb");
        thumb.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                .setPosition(YogaEdge.LEFT, 0f)
                .setPosition(YogaEdge.TOP, (float) thumbY)
                .setWidth(BoardMetrics.SCROLL_W).setHeight(thumbH));
        thumb.style(s -> s.background(TechTreeTextures.scrollThumb()));
        rail.addChild(thumb);
        return rail;
    }

    private void rebuildDetail(@Nullable TreeSnapshot.NodeView node) {
        clear(detailLayer);
        if (node == null) return;
        detailLayer.addChild(TechTreeDetailPanel.build(node, ClientTreeState.unlocked(node.location())));
    }

    private void clear(UIElement element) {
        for (UIElement child : new ArrayList<>(element.getChildren())) element.removeChild(child);
    }

    /** Drawable node locations, rebuilt from the snapshot and its edge list. */
    private void recomputeVisible() {
        visible.clear();
        TreeSnapshot snapshot = ClientTreeState.snapshot();
        for (TreeSnapshot.NodeView node : snapshot.nodes.values()) {
            if (ClientTreeState.visible(node.location())) visible.add(node.location());
        }
        visible.addAll(snapshot.progress.visibleNodes);
        visible.addAll(snapshot.progress.unlocked);
        // A node whose predecessor is unlocked is reachable, so it is drawn even when the server never listed
        // it: visibility is only pushed for nodes that start hidden.
        for (TreeSnapshot.NodeView node : snapshot.nodes.values()) {
            for (NodeLocation prev : node.prev()) {
                if (snapshot.progress.unlocked.contains(prev)) {
                    visible.add(node.location());
                    break;
                }
            }
        }
    }
}
