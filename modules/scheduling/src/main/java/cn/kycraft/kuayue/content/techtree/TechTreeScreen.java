package cn.kycraft.kuayue.content.techtree;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import cn.kycraft.kuayue.content.techtree.net.TechTreeRpc;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Blueprint table screen, laid out on the original NeoKuayue texture sheet.
 *
 * <p>Board is 300x150 like {@code blueprint_table.png}; the left strip lists published groups, the
 * centre draws the selected group's tech tree and the right pane shows the focused node.</p>
 */
public final class TechTreeScreen extends ModularUIScreen {

    static final int BOARD_W = 300;
    static final int BOARD_H = 150;
    private static final int GRID_X = 33;
    private static final int GRID_Y = 15;
    private static final int GRID_W = 247;
    private static final int GRID_H = 117;
    private static final int CELL = 20;

    private final BlueprintTableBlockEntity table;
    private ResourceLocation group;
    private NodeLocation focused;
    private UIElement detail;

    public TechTreeScreen(BlueprintTableBlockEntity table) {
        super(ModularUI.of(UI.of(new UIElement())), Component.translatable("tech_tree.board.title"));
        this.table = table;
    }

    /** The screen currently shown, so element-level buttons can drive navigation. */
    private static TechTreeScreen active;

    /** Called by the detail pane's back button. */
    public static void requestBackToTree() {
        TechTreeScreen screen = active;
        if (screen != null) {
            net.minecraft.client.Minecraft.getInstance().execute(screen::backToTree);
        }
    }

    /** Escape / right-click inside the detail view returns to the tree. */
    public void backToTree() {
        this.focused = null;
        this.init();
    }

    public static void open(BlueprintTableBlockEntity table) {
        if (Constants0.ready()) {
            TechTreeScreen screen = new TechTreeScreen(table);
            active = screen;
            Minecraft.getInstance().setScreen(screen);
        }
    }

    @Override
    public void removed() {
        if (active == this) active = null;
        super.removed();
    }

    @Override
    public void init() {
        // ModularUI centres itself from the root element size, and ModularUIScreen computes leftPos/topPos
        // during super.init(). The root must therefore already have its final size here, otherwise the
        // board is laid out at the default size and lands in the bottom-right corner.
        UIElement root = this.modularUI.ui.rootElement;
        root.layout(l -> l.setWidth(BOARD_W).setHeight(BOARD_H));
        root.style(s -> s.background(TechTreeTextures.board()));

        super.init();
        this.modularUI.setScreenAndInit(this);
        // getChildren() is an unmodifiable view: clear() throws UnsupportedOperationException, which
        // Screen.resize() hits when the window goes fullscreen and re-runs init(). Remove one by one.
        for (UIElement child : new java.util.ArrayList<>(root.getChildren())) {
            root.removeChild(child);
        }

        if (!ClientTreeState.groups().isEmpty()) {
            group = ClientTreeState.selectedGroup() != null
                    ? ClientTreeState.selectedGroup()
                    : ClientTreeState.groups().keySet().iterator().next();
        }

        // The node detail pane occupies the same area as the tree grid, so the two views are
        // mutually exclusive (same as the reference screen: picking a node switches to the detail view).
        boolean detailView = focused != null && ClientTreeState.nodes().containsKey(focused);

        if (!detailView) {
            root.addChild(treeGrid());
            root.addChild(progressArrow());
            root.addChild(groupStrip());
        }

        detail = null;
        if (detailView) {
            TreeSnapshot.NodeView node = ClientTreeState.nodes().get(focused);
            detail = TechTreeDetailPanel.build(node, ClientTreeState.unlocked(focused));
            root.addChild(detail);
        }
        if (detail == null) {
            root.addChild(unlockBar());
        }
    }

    /** Main unlock arrow: the reference draws it at (150,114) 53x15, filled by group progress. */
    private UIElement progressArrow() {
        UIElement arrow = new UIElement();
        arrow.layout(l -> l.setPositionType(org.appliedenergistics.yoga.YogaPositionType.ABSOLUTE)
                .setWidth(53).setHeight(15)
                .setPosition(org.appliedenergistics.yoga.YogaEdge.LEFT, 150f)
                .setPosition(org.appliedenergistics.yoga.YogaEdge.TOP, 114f));
        float progress = ClientTreeState.arrowProgress();
        arrow.style(style -> style.background(TechTreeTextures.progressBar()));
        arrow.layout(l -> l.setWidth(Math.max(1, Math.round(53 * progress))).setHeight(15)
                .setPosition(org.appliedenergistics.yoga.YogaEdge.LEFT, 150f)
                .setPosition(org.appliedenergistics.yoga.YogaEdge.TOP, 114f));
        return arrow;
    }

    /** Left column: one icon button per published group. */
    private UIElement groupStrip() {
        UIElement strip = new UIElement();
        strip.layout(l -> l.setPositionType(org.appliedenergistics.yoga.YogaPositionType.ABSOLUTE)
                .setWidth(28).setHeight(BOARD_H - 20)
                .setPosition(org.appliedenergistics.yoga.YogaEdge.LEFT, 4f)
                .setPosition(org.appliedenergistics.yoga.YogaEdge.TOP, 10f)
                .setFlexDirection(org.appliedenergistics.yoga.YogaFlexDirection.COLUMN)
                .setJustifyContent(org.appliedenergistics.yoga.YogaJustify.FLEX_START));
        strip.style(s -> s.zIndex(1));
        int index = 0;
        for (ResourceLocation id : ClientTreeState.groups().keySet()) {
            if (index++ >= 6) break;
            TreeSnapshot.GroupView group = ClientTreeState.groups().get(id);
            UIElement button = new UIElement();
            button.layout(l -> l.setWidth(24).setHeight(24));
            button.style(s -> s.background(TechTreeTextures.nodePlateDark(false))
                    .overlay(group.icon().isEmpty() ? null
                            : new com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture(group.icon())));
            button.addEventListener(UIEvents.CLICK, event -> {
                this.group = id;
                ClientTreeState.selectGroup(id);
                this.init();
            });
            strip.addChild(button);
        }
        return strip;
    }

    /** Centre: the group's nodes on the original 247x117 grid area. */
    private UIElement treeGrid() {
        UIElement grid = new UIElement();
        grid.layout(l -> l.setPositionType(org.appliedenergistics.yoga.YogaPositionType.ABSOLUTE)
                .setWidth(GRID_W).setHeight(GRID_H)
                .setPosition(org.appliedenergistics.yoga.YogaEdge.LEFT, (float) GRID_X)
                .setPosition(org.appliedenergistics.yoga.YogaEdge.TOP, (float) GRID_Y));
        grid.style(s -> s.zIndex(0));
        if (group == null) {
            return grid;
        }

        TreeSnapshot.GroupView view = ClientTreeState.groups().get(group);
        if (view != null) {
            Label title = new Label();
            title.setText(Component.translatable(view.nameKey()));
            title.textStyle(t -> t.textColor(0xFFFFFF).adaptiveHeight(true));
            title.layout(l -> l.setWidth(GRID_W).setHeight(12));
            grid.addChild(title);
        }

        ClientTreeState.layout(group).forEach((location, columnRow) -> {
            if (!ClientTreeState.visible(location)) return;
            TreeSnapshot.NodeView node = ClientTreeState.nodes().get(location);
            if (node == null) return;
            int x = Math.min(GRID_W - CELL, Math.max(0, columnRow[0] * (CELL + 6)));
            int y = 14 + Math.min(GRID_H - CELL - 14, Math.max(0, columnRow[1] * (CELL + 4)));
            grid.addChild(nodePlate(node, x, y));
        });
        return grid;
    }

    private UIElement nodePlate(TreeSnapshot.NodeView node, int x, int y) {
        boolean unlocked = ClientTreeState.unlocked(node.location());
        boolean epic = node.type() == NodeType.EPIC;
        UIElement plate = new UIElement();
        plate.layout(l -> l.setPositionType(org.appliedenergistics.yoga.YogaPositionType.ABSOLUTE)
                .setWidth(CELL).setHeight(CELL)
                .setPosition(org.appliedenergistics.yoga.YogaEdge.LEFT, (float) x)
                .setPosition(org.appliedenergistics.yoga.YogaEdge.TOP, (float) y));
        plate.style(s -> s.background(unlocked
                        ? TechTreeTextures.nodePlate(epic, true)
                        : TechTreeTextures.nodePlateDark(epic))
                .overlay(node.logo().isEmpty() ? null
                        : new com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture(node.logo()))
                .tooltips(Component.translatable(node.nameKey()),
                        Component.translatable(node.descriptionKey())));
        plate.addEventListener(UIEvents.CLICK, event -> {
            focused = node.location();
            this.init();
            TechTreeRpc.canUnlock().request(node.location().toString())
                    .thenAccept(result -> {
                        ClientTreeState.applyCheck(node.location(), result);
                        net.minecraft.client.Minecraft.getInstance().execute(this::init);
                    });
        });
        return plate;
    }

    /** Bottom bar: unlock confirm button, drawn from the original confirm sprite pair. */
    private UIElement unlockBar() {
        UIElement bar = new UIElement();
        bar.layout(l -> l.setPositionType(org.appliedenergistics.yoga.YogaPositionType.ABSOLUTE)
                .setWidth(16).setHeight(16)
                .setPosition(org.appliedenergistics.yoga.YogaEdge.LEFT, 168f)
                .setPosition(org.appliedenergistics.yoga.YogaEdge.TOP, 124f));
        bar.style(s -> s.background(TechTreeTextures.accent())
                .overlay(TechTreeTextures.outline(0xFFFFFFFF))
                .tooltips(Component.translatable("tech_tree.board.confirm")));
        return bar;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** In the detail view, Escape returns to the tree instead of closing the whole screen. */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE && focused != null) {
            backToTree();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /** Right click anywhere also backs out of the detail view. */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1 && focused != null) {
            backToTree();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** Guard: only build the screen when the client has a snapshot and LDLib2 is present. */
    private static final class Constants0 {
        private Constants0() {
        }

        static boolean ready() {
            return Minecraft.getInstance().player != null && !ClientTreeState.groups().isEmpty();
        }
    }
}
