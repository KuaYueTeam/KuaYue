package cn.kycraft.kuayue.content.tech_tree;

import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Right-hand node detail pane of the blueprint table.
 *
 * <p>Geometry is copied from the reference screen: eight consumption slots around (55..119, 105..113),
 * four result slots at (216..247, 105..113), the experience bar at (174, 113) and the finished
 * description block at (53, 107) sized 212x33.</p>
 *
 * <p>The pane is drawn on a board that is not a container menu, so its slots are painted stacks rather
 * than interactive {@code ItemSlot}s. Unlocking consumes from the player's inventory on the server
 * ({@link ServerUnlock}), and the {@code can_unlock} answer colours each stack red when it is missing —
 * with no menu there is no slot to put the items into, so an interactive slot would only invite a player
 * to move items into a store the server never reads.</p>
 */
final class TechTreeDetailPanel {

    private static final int SLOT = 16;
    /**
     * Painted size of an item icon inside a slot, relative to the 16x16 item frame.
     *
     * <p>The icons on the board came out of the game's own item renderer at 100%; at the board's small
     * pixel scale they read as a solid block, so they are drawn at 80% and centred in the slot.</p>
     */
    private static final float ICON_SCALE = 0.75f;

    /** Back arrow sprite and its hit rect. The arrow is 16x8 at (6,6); the hit box is padded to a
     *  comfort click target and stays inside the pane. */
    static final int[] BACK_HIT = {2, 2, 24, 18};
    /** Confirm sprite (16x16 at 168,124) and its padded hit rect. */
    /** Confirm sprite is a 16x16 square; the hit rect is padded around it. */
    static final int[] CONFIRM_HIT = {165, 113, 22, 22};

    private static final int CONSUME_CENTER_X = 71;
    private static final int CONSUME_CENTER_Y = 105;
    private static final int RESULT_CENTER_X = 231;
    private static final int RESULT_CENTER_Y = 105;

    private TechTreeDetailPanel() {
    }

    static UIElement build(TreeSnapshot.NodeView node, boolean unlocked) {
        CheckResult check = ClientTreeState.checkedNode() != null
                && ClientTreeState.checkedNode().equals(node.location())
                ? ClientTreeState.lastCheck()
                : null;
        UIElement pane = new UIElement();
        pane.setId("tech_tree_detail");
        pane.layout(l -> l.setPositionType(org.appliedenergistics.yoga.YogaPositionType.ABSOLUTE)
                .setWidth(BoardMetrics.WIDTH).setHeight(BoardMetrics.HEIGHT));

        // Opaque backdrop first: the detail pane is mounted over the tree view, and without this the node
        // plates and their state dots showed through the text. The sheet frame is drawn on top of the fill
        // so the pane still reads as the same blueprint paper as the board.
        UIElement backdrop = new UIElement();
        backdrop.setId("tech_tree_detail_backdrop");
        backdrop.layout(l -> l.setPositionType(org.appliedenergistics.yoga.YogaPositionType.ABSOLUTE)
                .setPosition(org.appliedenergistics.yoga.YogaEdge.LEFT, 0f)
                .setPosition(org.appliedenergistics.yoga.YogaEdge.TOP, 0f)
                .setWidth(BoardMetrics.WIDTH).setHeight(BoardMetrics.HEIGHT));
        backdrop.style(s -> s.background(TechTreeTextures.boardPaper()));
        pane.addChild(backdrop);

        UIElement sheet = new UIElement();
        sheet.setId("tech_tree_detail_sheet");
        sheet.layout(l -> l.setPositionType(org.appliedenergistics.yoga.YogaPositionType.ABSOLUTE)
                .setPosition(org.appliedenergistics.yoga.YogaEdge.LEFT, 0f)
                .setPosition(org.appliedenergistics.yoga.YogaEdge.TOP, 0f)
                .setWidth(BoardMetrics.WIDTH).setHeight(BoardMetrics.HEIGHT));
        sheet.style(s -> s.background(TechTreeTextures.tableSheet()));
        pane.addChild(sheet);

        pane.addChild(nodeTitle(node));
        pane.addChild(backButton());
        pane.addChild(descriptionLabel(node));
        // Widgets without a listener never reach the router: the world-UI backend only registers an input
        // surface for GuiEventListeners, and a bare UIElement is not one. Every clickable sprite therefore
        // gets an invisible hit area on top of it.
        pane.addChild(hitArea(BACK_HIT));
        if (unlocked) {
            pane.addChild(finishedTooltip(node));
            return pane;
        }
        pane.addChild(consumptionSlots(node, check));
        pane.addChild(resultSlots(node, check));
        pane.addChild(experienceBar(node, check));
        if (check == null || check.unlockable()) {
            pane.addChild(confirmButton(node));
            pane.addChild(hitArea(CONFIRM_HIT));
        }
        return pane;
    }

    /** Visible way back to the tree view; the reference screen uses the same arrow sprite. */
    private static UIElement backButton() {
        UIElement button = new UIElement();
        button.layout(l -> l.setPositionType(org.appliedenergistics.yoga.YogaPositionType.ABSOLUTE)
                .setWidth(16).setHeight(8)
                .setPosition(org.appliedenergistics.yoga.YogaEdge.LEFT, (float) BACK_HIT[0] + 8)
                .setPosition(org.appliedenergistics.yoga.YogaEdge.TOP, (float) BACK_HIT[1] + 5));
        button.style(s -> s.background(TechTreeTextures.backArrow())
                .tooltips(Component.translatable("tech_tree.board.back")));
        return button;
    }

    /** Invisible click target over a painted sprite: element listener, {@code onBack} / {@code onConfirm}. */
    private static UIElement hitArea(int[] rect) {
        UIElement area = new UIElement();
        area.setId("tech_tree_hit_" + rect[0] + "_" + rect[1]);
        area.layout(l -> l.setPositionType(org.appliedenergistics.yoga.YogaPositionType.ABSOLUTE)
                .setPosition(org.appliedenergistics.yoga.YogaEdge.LEFT, (float) rect[0])
                .setPosition(org.appliedenergistics.yoga.YogaEdge.TOP, (float) rect[1])
                .setWidth(rect[2]).setHeight(rect[3]));
        area.addEventListener(UIEvents.CLICK, event -> {
            if (rect[0] == BACK_HIT[0]) {
                TechTreeScreen.requestBackToTree();
            } else {
                NodeLocation focused = TechTreeScreen.getMenuFocus();
                TreeSnapshot.NodeView node = focused == null ? null : ClientTreeState.nodes().get(focused);
                if (node != null) TechTreeTableSurface.confirm(node);
            }
            event.stopPropagation();
        });
        return area;
    }

    private static UIElement nodeTitle(TreeSnapshot.NodeView node) {
        Label title = new Label();
        title.setText(Component.translatable(node.nameKey()));
        title.textStyle(t -> t.textColor(0xFFFFFFFF).adaptiveHeight(true));
        title.layout(l -> l.setPositionType(org.appliedenergistics.yoga.YogaPositionType.ABSOLUTE)
                .setWidth(220).setHeight(10)
                .setPosition(org.appliedenergistics.yoga.YogaEdge.LEFT, 39f)
                .setPosition(org.appliedenergistics.yoga.YogaEdge.TOP, 16f));
        return title;
    }

    /**
     * Node description, always visible so the detail page explains what the node is.
     *
     * <p>The description sits under the title and above the slot row; the completed-view text block is a
     * separate element and only appears once the node is unlocked.</p>
     */
    private static UIElement descriptionLabel(TreeSnapshot.NodeView node) {
        Label label = new Label();
        label.setText(Component.translatable(node.descriptionKey()));
        label.textStyle(t -> t.textColor(0xFFD8C9A8)
                .adaptiveWidth(false)
                .adaptiveHeight(true)
                .textWrap(com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap.WRAP));
        label.layout(l -> l.setPositionType(org.appliedenergistics.yoga.YogaPositionType.ABSOLUTE)
                .setWidth(212).setHeight(28)
                .setPosition(org.appliedenergistics.yoga.YogaEdge.LEFT, 39f)
                .setPosition(org.appliedenergistics.yoga.YogaEdge.TOP, 28f));
        return label;
    }

    /**
     * Consumption stacks, laid out like the reference screen: one centre stack, a 3x2 block and a
     * closing stack. Each stack is painted with a green/red mark from the last pre-check answer.
     */
    private static UIElement consumptionSlots(TreeSnapshot.NodeView node, CheckResult check) {
        UIElement group = new UIElement();
        group.layout(l -> l.setPositionType(org.appliedenergistics.yoga.YogaPositionType.ABSOLUTE)
                .setWidth(BoardMetrics.WIDTH).setHeight(BoardMetrics.HEIGHT));
        List<ItemStack> consume = node.consume();
        for (int i = 0; i < 8; i++) {
            int x;
            int y;
            if (i == 0) {
                x = 55;
                y = 113;
            } else if (i == 7) {
                x = 119;
                y = 113;
            } else {
                x = CONSUME_CENTER_X + ((i - 1) / 2) * SLOT;
                y = CONSUME_CENTER_Y + ((i - 1) % 2) * SLOT;
            }
            ItemStack shown = i < consume.size() ? consume.get(i) : ItemStack.EMPTY;
            boolean satisfied = check != null && !missingItem(check, shown);
            group.addChild(stack(shown, x, y, satisfied));
        }
        return group;
    }

    private static UIElement resultSlots(TreeSnapshot.NodeView node, CheckResult check) {
        UIElement group = new UIElement();
        group.layout(l -> l.setPositionType(org.appliedenergistics.yoga.YogaPositionType.ABSOLUTE)
                .setWidth(BoardMetrics.WIDTH).setHeight(BoardMetrics.HEIGHT));
        List<ItemStack> results = check != null && !check.rewards().isEmpty()
                ? check.rewards() : node.rewards();
        int[][] positions = {{216, 113}, {231, 105}, {231, 121}, {247, 113}};
        for (int i = 0; i < Math.min(positions.length, results.size()); i++) {
            group.addChild(stack(results.get(i), positions[i][0], positions[i][1], false));
        }
        return group;
    }

    /**
     * One painted stack.
     *
     * <p>An empty stack still gets its frame, so the two blank corners of the consumption row read as
     * empty slots instead of holes in the sheet.</p>
     */
    private static UIElement stack(ItemStack stack, int x, int y, boolean satisfied) {
        UIElement slot = new UIElement();
        slot.setId("stack_" + x + "_" + y);
        slot.layout(l -> l.setPositionType(org.appliedenergistics.yoga.YogaPositionType.ABSOLUTE)
                .setWidth(SLOT).setHeight(SLOT)
                .setPosition(org.appliedenergistics.yoga.YogaEdge.LEFT, (float) x)
                .setPosition(org.appliedenergistics.yoga.YogaEdge.TOP, (float) y));
        slot.style(s -> s.background(TechTreeTextures.slot()));
        if (stack != null && !stack.isEmpty()) {
            String count = stack.getCount() > 1 ? " x" + stack.getCount() : "";
            slot.style(s -> s.overlay(TechTreeTextures.slotMark(satisfied))
                    .tooltips(Component.translatable("%s%s", stack.getHoverName(), count)));
            // The item icon is its own element so it can be painted smaller than the slot: drawn at full
            // 16x16 the icons covered the slot frame and read as a solid block at the board's pixel scale.
            UIElement icon = new UIElement();
            int size = Math.round(16 * ICON_SCALE);
            int inset = (SLOT - size) / 2;
            icon.layout(l -> l.setPositionType(org.appliedenergistics.yoga.YogaPositionType.ABSOLUTE)
                    .setPosition(org.appliedenergistics.yoga.YogaEdge.LEFT, (float) inset)
                    .setPosition(org.appliedenergistics.yoga.YogaEdge.TOP, (float) inset)
                    .setWidth(size).setHeight(size));
            icon.style(s -> s.overlay(new ItemStackTexture(stack)));
            slot.addChild(icon);
        }
        return slot;
    }

    /** The server listed this stack as missing, so the stack is painted red instead of green. */
    private static boolean missingItem(CheckResult check, ItemStack required) {
        if (check == null || required.isEmpty()) return false;
        return check.missingItems().stream().anyMatch(stack -> ItemStack.isSameItemSameComponents(stack, required));
    }

    /**
     * Experience requirement, drawn like 1.0 {@code ExpComponentBar}: a 9x11 bar plus the level number to
     * its right, green when the player has it and red when not.
     */
    private static UIElement experienceBar(TreeSnapshot.NodeView node, CheckResult check) {
        boolean enough = check != null ? check.enoughLevel() : enoughExperience(node);
        UIElement group = new UIElement();
        group.layout(l -> l.setPositionType(org.appliedenergistics.yoga.YogaPositionType.ABSOLUTE)
                .setWidth(40).setHeight(11)
                .setPosition(org.appliedenergistics.yoga.YogaEdge.LEFT, 174f)
                .setPosition(org.appliedenergistics.yoga.YogaEdge.TOP, 108f));

        UIElement bar = new UIElement();
        bar.layout(l -> l.setPositionType(org.appliedenergistics.yoga.YogaPositionType.ABSOLUTE)
                .setPosition(org.appliedenergistics.yoga.YogaEdge.LEFT, 0f)
                .setPosition(org.appliedenergistics.yoga.YogaEdge.TOP, 0f)
                .setWidth(9).setHeight(11));
        bar.style(s -> s.background(TechTreeTextures.expBar(enough)));
        group.addChild(bar);

        if (node.level() > 0) {
            Label level = new Label();
            level.setText(Component.literal(String.valueOf(node.level())));
            level.textStyle(t -> t.textColor(enough ? 0xFF55FF55 : 0xFFFF5555).adaptiveHeight(true));
            level.layout(l -> l.setPositionType(org.appliedenergistics.yoga.YogaPositionType.ABSOLUTE)
                    .setPosition(org.appliedenergistics.yoga.YogaEdge.LEFT, 12f)
                    .setPosition(org.appliedenergistics.yoga.YogaEdge.TOP, 1f)
                    .setWidth(28).setHeight(10));
            group.addChild(level);
        }
        return group;
    }

    private static boolean enoughExperience(TreeSnapshot.NodeView node) {
        var player = net.minecraft.client.Minecraft.getInstance().player;
        if (player == null) return false;
        return player.experienceLevel >= node.level() && player.totalExperience >= node.exp();
    }

    /**
     * Confirm button: the unlock itself, answered with the authoritative snapshot.
     *
     * <p>Sprite and behaviour follow 1.0: the 16x16 frame doubles as the hover state (the darker second
     * frame of the same sheet strip), and the click itself is handled by {@link #hitArea(int[])} on top.</p>
     */
    private static UIElement confirmButton(TreeSnapshot.NodeView node) {
        UIElement button = new UIElement();
        button.setId("tech_tree_confirm");
        button.layout(l -> l.setPositionType(org.appliedenergistics.yoga.YogaPositionType.ABSOLUTE)
                .setWidth(16).setHeight(16)
                .setPosition(org.appliedenergistics.yoga.YogaEdge.LEFT, (float) CONFIRM_HIT[0] + 3)
                .setPosition(org.appliedenergistics.yoga.YogaEdge.TOP, (float) CONFIRM_HIT[1] + 3));
        button.style(s -> s.background(TechTreeTextures.confirm()));
        button.addEventListener(UIEvents.MOUSE_ENTER, event -> button.style(s -> s.background(
                TechTreeTextures.confirmHover())));
        button.addEventListener(UIEvents.MOUSE_LEAVE, event -> button.style(s -> s.background(
                TechTreeTextures.confirm())));
        return button;
    }

    /** Unlocked node: description block at (53,107) 212x33 with the completed stamp. */
    private static UIElement finishedTooltip(TreeSnapshot.NodeView node) {
        UIElement block = new UIElement();
        block.layout(l -> l.setPositionType(org.appliedenergistics.yoga.YogaPositionType.ABSOLUTE)
                .setWidth(212).setHeight(33)
                .setPosition(org.appliedenergistics.yoga.YogaEdge.LEFT, 53f)
                .setPosition(org.appliedenergistics.yoga.YogaEdge.TOP, 107f));
        // The stamp is a 32x32 sprite; stretching it across 212x33 widened the check mark.
        UIElement stamp = new UIElement();
        stamp.layout(l -> l.setPositionType(org.appliedenergistics.yoga.YogaPositionType.ABSOLUTE)
                .setWidth(32).setHeight(32));
        stamp.style(s -> s.background(TechTreeTextures.completedStamp()));
        block.addChild(stamp);

        String text = node.unlockKey() == null || node.unlockKey().isEmpty()
                ? node.descriptionKey()
                : node.unlockKey();
        Label label = new Label();
        label.setText(Component.translatable(text));
        label.textStyle(t -> t.textColor(0xFFFFFFFF).adaptiveWidth(false).adaptiveHeight(true)
                .textWrap(com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap.WRAP));
        label.layout(l -> l.setWidth(212 - 45).setHeight(33)
                .setPosition(org.appliedenergistics.yoga.YogaEdge.LEFT, 38f));
        block.addChild(label);
        return block;
    }
}
