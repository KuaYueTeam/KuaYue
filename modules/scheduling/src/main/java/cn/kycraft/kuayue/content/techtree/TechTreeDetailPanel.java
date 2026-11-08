package cn.kycraft.kuayue.content.techtree;

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
 */
final class TechTreeDetailPanel {

    private static final int SLOT = 16;
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
        pane.layout(l -> l.setPositionType(org.appliedenergistics.yoga.YogaPositionType.ABSOLUTE)
                .setWidth(TechTreeScreen.BOARD_W).setHeight(TechTreeScreen.BOARD_H));

        pane.addChild(nodeTitle(node));
        pane.addChild(backButton());
        if (unlocked) {
            pane.addChild(finishedTooltip(node));
            return pane;
        }
        pane.addChild(consumptionSlots(node, check));
        pane.addChild(resultSlots(node, check));
        pane.addChild(experienceBar(node, check));
        if (check == null || check.unlockable()) {
            pane.addChild(confirmButton(node));
        }
        return pane;
    }

    /** Visible way back to the tree view; the reference screen uses the same arrow sprite. */
    private static UIElement backButton() {
        UIElement button = new UIElement();
        button.layout(l -> l.setPositionType(org.appliedenergistics.yoga.YogaPositionType.ABSOLUTE)
                .setWidth(16).setHeight(8).setPositionType(org.appliedenergistics.yoga.YogaPositionType.ABSOLUTE)
                .setPosition(org.appliedenergistics.yoga.YogaEdge.LEFT, 6f)
                .setPosition(org.appliedenergistics.yoga.YogaEdge.TOP, 6f));
        button.style(s -> s.background(TechTreeTextures.accent())
                .tooltips(Component.translatable("tech_tree.board.back")));
        button.addEventListener(com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents.CLICK,
                event -> TechTreeScreen.requestBackToTree());
        return button;
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

    private static UIElement consumptionSlots(TreeSnapshot.NodeView node, CheckResult check) {
        UIElement group = new UIElement();
        group.layout(l -> l.setPositionType(org.appliedenergistics.yoga.YogaPositionType.ABSOLUTE)
                .setWidth(TechTreeScreen.BOARD_W).setHeight(TechTreeScreen.BOARD_H));
        List<ItemStack> consume = node.consume();
        for (int i = 0; i < Math.min(8, consume.size()); i++) {
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
            group.addChild(slot(consume.get(i), x, y, check == null || !missingItem(check, consume.get(i))));
        }
        return group;
    }

    private static UIElement resultSlots(TreeSnapshot.NodeView node, CheckResult check) {
        UIElement group = new UIElement();
        group.layout(l -> l.setPositionType(org.appliedenergistics.yoga.YogaPositionType.ABSOLUTE)
                .setWidth(TechTreeScreen.BOARD_W).setHeight(TechTreeScreen.BOARD_H));
        List<ItemStack> results = check != null && !check.rewards().isEmpty()
                ? check.rewards() : node.rewards();
        int[][] positions = {{216, 113}, {231, 105}, {231, 121}, {247, 113}};
        for (int i = 0; i < Math.min(positions.length, results.size()); i++) {
            group.addChild(slot(results.get(i), positions[i][0], positions[i][1], false));
        }
        return group;
    }

    private static UIElement slot(ItemStack stack, int x, int y, boolean satisfied) {
        UIElement slot = new UIElement();
        slot.layout(l -> l.setPositionType(org.appliedenergistics.yoga.YogaPositionType.ABSOLUTE)
                .setWidth(SLOT).setHeight(SLOT)
                .setPosition(org.appliedenergistics.yoga.YogaEdge.LEFT, (float) x)
                .setPosition(org.appliedenergistics.yoga.YogaEdge.TOP, (float) y));
        slot.style(s -> s.background(TechTreeTextures.slot(satisfied))
                .overlay(stack.isEmpty() ? null : new ItemStackTexture(stack))
                .overlay(satisfied ? TechTreeTextures.slotMark(true) : null)
                .tooltips(stack.isEmpty() ? new Component[0] : new Component[]{stack.getHoverName()}));
        return slot;
    }

    /** The server listed this stack as missing, so the slot is painted red instead of green. */
    private static boolean missingItem(CheckResult check, ItemStack required) {
        if (check == null || required.isEmpty()) return false;
        return check.missingItems().stream().anyMatch(stack -> ItemStack.isSameItemSameComponents(stack, required));
    }

    private static UIElement experienceBar(TreeSnapshot.NodeView node, CheckResult check) {
        boolean enough = check != null ? check.enoughLevel() : enoughExperience(node);
        UIElement bar = new UIElement();
        bar.layout(l -> l.setPositionType(org.appliedenergistics.yoga.YogaPositionType.ABSOLUTE)
                .setWidth(9).setHeight(11)
                .setPosition(org.appliedenergistics.yoga.YogaEdge.LEFT, 174f)
                .setPosition(org.appliedenergistics.yoga.YogaEdge.TOP, 108f));
        bar.style(s -> s.background(enough ? TechTreeTextures.slotMark(true) : TechTreeTextures.slotMark(false)));
        return bar;
    }

    private static boolean enoughExperience(TreeSnapshot.NodeView node) {
        var player = net.minecraft.client.Minecraft.getInstance().player;
        if (player == null) return false;
        return player.experienceLevel >= node.level() && player.totalExperience >= node.exp();
    }

    private static UIElement confirmButton(TreeSnapshot.NodeView node) {
        UIElement button = new UIElement();
        button.layout(l -> l.setPositionType(org.appliedenergistics.yoga.YogaPositionType.ABSOLUTE)
                .setWidth(16).setHeight(16)
                .setPosition(org.appliedenergistics.yoga.YogaEdge.LEFT, 168f)
                .setPosition(org.appliedenergistics.yoga.YogaEdge.TOP, 124f));
        button.style(s -> s.background(TechTreeTextures.accent()).overlay(TechTreeTextures.outline(0xFFFFFFFF)));
        button.addEventListener(UIEvents.CLICK, event -> {
            cn.kycraft.kuayue.content.techtree.net.TechTreeRpc.unlockNode()
                    .request(node.location().toString())
                    .thenAccept(ClientTreeState::apply);
        });
        return button;
    }

    /** Unlocked node: description block at (53,107) 212x33 with the completed stamp. */
    private static UIElement finishedTooltip(TreeSnapshot.NodeView node) {
        UIElement block = new UIElement();
        block.layout(l -> l.setPositionType(org.appliedenergistics.yoga.YogaPositionType.ABSOLUTE)
                .setWidth(212).setHeight(33)
                .setPosition(org.appliedenergistics.yoga.YogaEdge.LEFT, 53f)
                .setPosition(org.appliedenergistics.yoga.YogaEdge.TOP, 107f));
        block.style(s -> s.background(TechTreeTextures.nodePlate(false, true)));

        String text = node.unlockKey() == null || node.unlockKey().isEmpty()
                ? node.descriptionKey()
                : node.unlockKey();
        Label label = new Label();
        label.setText(Component.translatable(text));
        label.textStyle(t -> t.textColor(0xFFFFFFFF).adaptiveHeight(true).textWrap(
                com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap.WRAP));
        label.layout(l -> l.setWidth(212 - 45).setHeight(33)
                .setPosition(org.appliedenergistics.yoga.YogaEdge.LEFT, 5f));
        block.addChild(label);
        return block;
    }
}
