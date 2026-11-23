package cn.kycraft.kuayue.content.tech_tree;

import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaPositionType;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Group publish panel, modelled on 1.0 {@code UnlockGroupBoard}.
 *
 * <p>A group with {@code initial_visibility = false} (the overhead-line tree) is not published by
 * default: its nodes stay locked until the player meets the group's {@code unlock_condition} and confirms.
 * 1.0 showed a 120x80 panel with the still-missing nodes in a 3x3 grid and a confirm button at (83,16);
 * this is the same panel, painted on the board instead of in a screen.</p>
 *
 * <p>The grid is the group's <em>incoming</em> nodes — the ones other groups declare in
 * {@code next_groups} — because those are exactly the predecessors {@code canPublish} counts. Each cell is
 * a 20x20 plate reusing the board's own node sprites, so the panel reads as part of the tree.</p>
 */
final class TechTreeGroupPublishPanel {

    /** Panel size, from the 1.0 sheet. */
    static final int WIDTH = 120;
    static final int HEIGHT = 80;

    /**
     * Panel origin.
     *
     * <p>Centred on the board's tree grid rather than the whole sheet: the board pushed the grid right to
     * clear the group strip, and the publish panel stands in for that grid, so it follows it.</p>
     */
    private static final int PANEL_X =
            TechTreeTableSurface.GRID_X + (TechTreeTableSurface.WIDTH - TechTreeTableSurface.GRID_X - WIDTH) / 2;
    private static final int PANEL_Y = (TechTreeTableSurface.HEIGHT - HEIGHT) / 2;
    private static final int SLOT = 21;
    private static final int CELL = 20;
    private static final int GRID_X = 15;
    private static final int GRID_Y = 9;

    /** Confirm sprite and hit rect inside the panel, from the 1.0 layout (83,16) 16x32 strip. */
    static final int[] CONFIRM_HIT = {PANEL_X + 82, PANEL_Y + 15, 18, 34};

    private TechTreeGroupPublishPanel() {
    }

    /** True when this group still needs a publish confirmation. */
    static boolean needsPublish(@Nullable ResourceLocation group) {
        if (group == null) return false;
        TreeSnapshot.GroupView view = ClientTreeState.groups().get(group);
        if (view == null) return false;
        if (ClientTreeState.progress().unlockedGroups.contains(group)) return false;
        return !ClientTreeState.initialVisibility(group);
    }

    /** Panel X/Y in board pixels, for the manual hit test. */
    static int[] rect() {
        return new int[]{PANEL_X, PANEL_Y, WIDTH, HEIGHT};
    }

    /**
     * Builds the panel for one group.
     *
     * @param onConfirm run by the panel's own confirm sprite
     */
    static UIElement build(ResourceLocation group, Runnable onConfirm) {
        TreeSnapshot.GroupView view = ClientTreeState.groups().get(group);
        UIElement panel = new UIElement();
        panel.setId("tech_tree_publish");
        panel.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                .setPosition(YogaEdge.LEFT, (float) PANEL_X)
                .setPosition(YogaEdge.TOP, (float) PANEL_Y)
                .setWidth(WIDTH).setHeight(HEIGHT));
        panel.style(s -> s.background(TechTreeTextures.unlockGroupBoard()));

        if (view == null) return panel;

        // Title strip: which group is being published.
        Label title = new Label();
        title.setText(Component.translatable(view.nameKey()));
        title.textStyle(t -> t.textColor(0xFF2A2018).adaptiveWidth(false).adaptiveHeight(true));
        title.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                .setPosition(YogaEdge.LEFT, (float) GRID_X)
                .setPosition(YogaEdge.TOP, 0f)
                .setWidth(WIDTH - GRID_X - 4).setHeight(10));
        panel.addChild(title);

        // Missing predecessors, 3x3 like 1.0 LabelGrid.
        List<NodeLocation> missing = missingIncoming(view);
        for (int i = 0; i < Math.min(missing.size(), 9); i++) {
            panel.addChild(cell(missing.get(i), i));
        }

        panel.addChild(confirm());
        panel.addChild(hitArea(CONFIRM_HIT, onConfirm));
        return panel;
    }

    /** Incoming nodes of the group that the player has not unlocked yet. */
    private static List<NodeLocation> missingIncoming(TreeSnapshot.GroupView view) {
        List<NodeLocation> missing = new ArrayList<>();
        for (NodeLocation incoming : view.incoming()) {
            if (!ClientTreeState.unlocked(incoming)) missing.add(incoming);
        }
        return missing;
    }

    /** One 20x20 predecessor plate, laid out on 1.0's 21px grid. */
    private static UIElement cell(NodeLocation location, int index) {
        UIElement element = new UIElement();
        element.setId("publish_" + location);
        element.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                .setPosition(YogaEdge.LEFT, (float) (GRID_X + (index % 3) * SLOT))
                .setPosition(YogaEdge.TOP, (float) (GRID_Y + (index / 3) * SLOT))
                .setWidth(CELL).setHeight(CELL));
        TreeSnapshot.NodeView node = ClientTreeState.nodes().get(location);
        if (node == null) {
            element.style(s -> s.background(TechTreeTextures.nodePlateDark(false)));
            return element;
        }
        element.style(s -> s.background(TechTreeTextures.nodePlate(node.type() == NodeType.EPIC, false))
                .overlay(node.logo().isEmpty() ? null : new ItemStackTexture(node.logo())));
        return element;
    }

    /** Confirm sprite at the 1.0 position; the panel already owns the click through its hit area. */
    private static UIElement confirm() {
        UIElement button = new UIElement();
        button.setId("tech_tree_publish_confirm");
        button.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                .setPosition(YogaEdge.LEFT, (float) (CONFIRM_HIT[0] - PANEL_X))
                .setPosition(YogaEdge.TOP, (float) (CONFIRM_HIT[1] - PANEL_Y))
                .setWidth(16).setHeight(32));
        button.style(s -> s.background(TechTreeTextures.confirm()));
        button.addEventListener(UIEvents.MOUSE_ENTER, event -> button.style(s -> s.background(
                TechTreeTextures.confirmHover())));
        button.addEventListener(UIEvents.MOUSE_LEAVE, event -> button.style(s -> s.background(
                TechTreeTextures.confirm())));
        return button;
    }

    /** Invisible click target over the confirm sprite; coordinates are board-local. */
    private static UIElement hitArea(int[] rect, Runnable onConfirm) {
        UIElement area = new UIElement();
        area.setId("tech_tree_publish_hit");
        area.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                .setPosition(YogaEdge.LEFT, (float) (rect[0] - PANEL_X))
                .setPosition(YogaEdge.TOP, (float) (rect[1] - PANEL_Y))
                .setWidth(rect[2]).setHeight(rect[3]));
        area.addEventListener(UIEvents.CLICK, event -> {
            onConfirm.run();
            event.stopPropagation();
        });
        return area;
    }
}
