package cn.kycraft.kuayue.content.tech_tree;

import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import net.minecraft.world.item.ItemStack;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaPositionType;

/**
 * Builds one node plate: the click target, the sprite, the item icon and the state dot.
 *
 * <p>Split out of {@link TechTreeTableSurface} so the drawing rules for a single node live together. The
 * caller supplies window-local pixels; nothing here knows about scrolling or the tree graph beyond reading
 * the snapshot for a node's logo and its predecessors.</p>
 */
final class BoardPlates {

    private BoardPlates() {
    }

    /**
     * One plate, positioned with its top-left at {@code (x, y)} in window-local pixels.
     *
     * <p>The outer element is the click target and is padded by {@link BoardMetrics#HIT_PAD} on every side,
     * so the sprite stays its own size while the hit box is a comfortable click.</p>
     */
    static UIElement build(TreeSnapshot.NodeView node, int x, int y, Runnable onFocus) {
        boolean unlocked = ClientTreeState.unlocked(node.location());
        boolean focused = node.location().equals(TechTreeScreen.getMenuFocus());

        UIElement element = new UIElement();
        element.setId("node_" + node.location());
        element.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                .setPosition(YogaEdge.LEFT, (float) (x - BoardMetrics.HIT_PAD))
                .setPosition(YogaEdge.TOP, (float) (y - BoardMetrics.HIT_PAD))
                .setWidth(BoardMetrics.CELL_HIT_W).setHeight(BoardMetrics.CELL_HIT_H));
        element.addEventListener(UIEvents.CLICK, event -> {
            onFocus.run();
            event.stopPropagation();
        });

        element.addChild(sprite());
        element.addChild(icon(node.logo()));
        element.addChild(stateDot(unlocked, hasMissingPredecessor(node), focused));
        return element;
    }

    /** The plate artwork itself, inset by the hit padding. */
    private static UIElement sprite() {
        UIElement sprite = new UIElement();
        sprite.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                .setPosition(YogaEdge.LEFT, (float) BoardMetrics.HIT_PAD)
                .setPosition(YogaEdge.TOP, (float) BoardMetrics.HIT_PAD)
                .setWidth(BoardMetrics.CELL_W).setHeight(BoardMetrics.CELL_H));
        sprite.style(s -> s.background(TechTreeTextures.nodePlateSprite()));
        return sprite;
    }

    /**
     * Item icon, centred on the plate.
     *
     * <p>The plate is wider than it is tall, so the two insets differ; a single inset value (the older
     * code) pushed the icon into the top-left corner.</p>
     */
    private static UIElement icon(ItemStack logo) {
        int size = Math.round(BoardMetrics.CELL_H * BoardMetrics.ICON_SCALE);
        int insetX = (BoardMetrics.CELL_W - size) / 2;
        int insetY = (BoardMetrics.CELL_H - size) / 2;

        UIElement item = new UIElement();
        item.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                .setPosition(YogaEdge.LEFT, (float) (BoardMetrics.HIT_PAD + insetX))
                .setPosition(YogaEdge.TOP, (float) (BoardMetrics.HIT_PAD + insetY))
                .setWidth(size).setHeight(size));
        if (!logo.isEmpty()) {
            item.style(s -> s.overlay(new ItemStackTexture(logo)));
        }
        return item;
    }

    /**
     * State dot in the plate's bottom-right corner, as 1.0 draws it.
     *
     * <p>Red while a predecessor is still locked, white otherwise. The dot is sized down with the plate:
     * 7px dominated a 16px-tall plate.</p>
     */
    private static UIElement stateDot(boolean unlocked, boolean required, boolean focused) {
        int size = 6;
        UIElement dot = new UIElement();
        dot.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                .setPosition(YogaEdge.LEFT, (float) (BoardMetrics.HIT_PAD + BoardMetrics.CELL_W - size - 2))
                .setPosition(YogaEdge.TOP, (float) (BoardMetrics.HIT_PAD + BoardMetrics.CELL_H - size - 1))
                .setWidth(size).setHeight(size));
        // A focused plate gets no dot: the detail pane already carries its state, and the marker would sit
        // under the cursor.
        dot.style(s -> s.background(focused
                ? com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture.EMPTY
                : TechTreeTextures.stateDot(required)));
        return dot;
    }

    /**
     * True when any predecessor of the node is still locked.
     *
     * <p>Read straight from the snapshot's edge list, so the dot is correct before the player clicks
     * anything; 1.0 computed the same flag when it built its labels.</p>
     */
    static boolean hasMissingPredecessor(TreeSnapshot.NodeView node) {
        for (NodeLocation prev : node.prev()) {
            if (!ClientTreeState.unlocked(prev)) return true;
        }
        return false;
    }
}
