package cn.kycraft.kuayue.content.tech_tree;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import cn.kycraft.kuayue.content.tech_tree.net.TechTreeRpc;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaFlexDirection;
import org.appliedenergistics.yoga.YogaJustify;
import org.appliedenergistics.yoga.YogaPositionType;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Focus bookkeeping of the blueprint board, and the optional screen host.
 *
 * <p>The board is normally painted on the table top by {@link TechTreeTableSurface}. Everything shared
 * between that host and the deprecated screen below lives here: which node is focused, which group is
 * selected, and the tree grid element that both hosts mount.</p>
 *
 * <p>This class no longer owns a container menu. The right click on the table opens the world-space
 * surface, so the 36-slot player inventory that used to be rebuilt here (with its deferred
 * {@code ItemSlot.bind} dance) is gone: the inventory is still the player's own GUI, and the tree only
 * ever needs read access to it, which the {@code can_unlock} answer already encodes.</p>
 */
public final class TechTreeScreen extends ModularUIScreen {

    static final int BOARD_W = 300;
    static final int BOARD_H = 150;

    /** Grid metrics, kept in sync with {@link TechTreeTableSurface}. */
    private static final int GRID_X = 33;
    private static final int GRID_Y = 15;
    private static final int GRID_W = 247;
    private static final int GRID_H = 117;
    private static final int CELL = 20;
    private static final int HEADER_H = 14;

    private final BlueprintTableBlockEntity table;
    private ResourceLocation group;
    private NodeLocation focused;
    private UIElement detail;

    /** The screen currently shown, so element-level buttons can drive navigation. */
    private static TechTreeScreen active;

    public TechTreeScreen(@Nullable BlueprintTableBlockEntity table) {
        super(ModularUI.of(UI.of(new UIElement())), Component.translatable("tech_tree.board.title"));
        this.table = table;
    }

    // --- shared focus state ------------------------------------------------------------------

    private static volatile NodeLocation menuFocus;

    static void setMenuFocus(@Nullable NodeLocation focus) {
        menuFocus = focus;
    }

    @Nullable
    static NodeLocation getMenuFocus() {
        return menuFocus;
    }

    static void clearMenuFocus() {
        menuFocus = null;
    }

    /** Called by the detail pane's back button, whichever host is showing. */
    public static void requestBackToTree() {
        if (TechTreeTableSurface.isMounted()) {
            TechTreeTableSurface.backToTree();
            return;
        }
        TechTreeScreen screen = active;
        if (screen != null) {
            Minecraft.getInstance().execute(screen::backToTree);
        }
    }

    /** Standalone screen host; kept for debugging outside a world-backed table. */
    public static void open(@Nullable BlueprintTableBlockEntity table) {
        if (Minecraft.getInstance().player == null || ClientTreeState.groups().isEmpty()) return;
        TechTreeScreen screen = new TechTreeScreen(table);
        active = screen;
        Minecraft.getInstance().setScreen(screen);
    }

    public void backToTree() {
        this.focused = null;
        clearMenuFocus();
        this.init();
    }

    @Override
    public void removed() {
        if (active == this) active = null;
        super.removed();
    }

    @Override
    public void init() {
        // ModularUI centres itself from the root element size, and ModularUIScreen computes leftPos/topPos
        // during super.init(). The root must therefore already have its final size here.
        UIElement root = this.modularUI.ui.rootElement;
        root.layout(l -> l.setWidth(BOARD_W).setHeight(BOARD_H));
        root.style(s -> s.background(TechTreeTextures.tableSheet()));

        super.init();
        this.modularUI.setScreenAndInit(this);
        // getChildren() is an unmodifiable view: clear() throws, which Screen.resize() hits on fullscreen.
        for (UIElement child : new java.util.ArrayList<>(root.getChildren())) {
            root.removeChild(child);
        }

        if (!ClientTreeState.groups().isEmpty()) {
            group = ClientTreeState.selectedGroup() != null
                    ? ClientTreeState.selectedGroup()
                    : ClientTreeState.groups().keySet().iterator().next();
        }

        focused = getMenuFocus();
        boolean detailView = focused != null && ClientTreeState.nodes().containsKey(focused);

        if (!detailView) {
            root.addChild(treeGrid());
            root.addChild(progressArrow());
            root.addChild(groupStrip());
            root.addChild(unlockBar());
        } else {
            detail = TechTreeDetailPanel.build(ClientTreeState.nodes().get(focused),
                    ClientTreeState.unlocked(focused));
            root.addChild(detail);
        }
    }

    /** Main unlock arrow: the reference draws it at (150,114) 53x15, filled by group progress. */
    private UIElement progressArrow() {
        float progress = ClientTreeState.arrowProgress();
        UIElement arrow = new UIElement();
        arrow.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                .setWidth(Math.max(1, Math.round(53 * progress))).setHeight(15)
                .setPosition(YogaEdge.LEFT, 150f)
                .setPosition(YogaEdge.TOP, 114f));
        arrow.style(style -> style.background(TechTreeTextures.progressArrow(progress)));
        return arrow;
    }

    /** Left column: one icon button per published group. */
    private UIElement groupStrip() {
        UIElement strip = new UIElement();
        strip.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                .setWidth(28).setHeight(BOARD_H - 20)
                .setPosition(YogaEdge.LEFT, 4f)
                .setPosition(YogaEdge.TOP, 10f)
                .setFlexDirection(YogaFlexDirection.COLUMN)
                .setJustifyContent(YogaJustify.FLEX_START));
        strip.style(s -> s.zIndex(1));
        int index = 0;
        for (ResourceLocation id : ClientTreeState.groups().keySet()) {
            if (index++ >= 6) break;
            TreeSnapshot.GroupView view = ClientTreeState.groups().get(id);
            UIElement button = new UIElement();
            button.layout(l -> l.setWidth(24).setHeight(24));
            button.style(s -> s.background(TechTreeTextures.nodePlateDark(false))
                    .overlay(view.icon().isEmpty() ? null
                            : new com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture(view.icon())));
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
        grid.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                .setWidth(GRID_W).setHeight(GRID_H)
                .setPosition(YogaEdge.LEFT, (float) GRID_X)
                .setPosition(YogaEdge.TOP, (float) GRID_Y));
        grid.style(s -> s.zIndex(0));
        if (group == null) {
            return grid;
        }

        TreeSnapshot.GroupView view = ClientTreeState.groups().get(group);
        if (view != null) {
            Label title = new Label();
            title.setText(Component.translatable(view.nameKey()));
            title.textStyle(t -> t.textColor(0xFFFFFF).adaptiveHeight(true));
            title.layout(l -> l.setWidth(GRID_W).setHeight(HEADER_H));
            grid.addChild(title);
        }

        Map<NodeLocation, int[]> positions = ClientTreeState.layout(group);
        int rows = 1;
        for (int[] columnRow : positions.values()) {
            rows = Math.max(rows, columnRow[1] + 1);
        }
        int viewHeight = GRID_H - HEADER_H;
        int contentHeight = Math.max(viewHeight, rows * (CELL + 4));

        ScrollerView scroller = new ScrollerView();
        scroller.scrollerStyle(st -> st
                .mode(com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode.VERTICAL)
                .horizontalScrollDisplay(com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay.NEVER)
                .verticalScrollDisplay(com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay.AUTO));
        scroller.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                .setWidth(GRID_W).setHeight(viewHeight)
                .setPosition(YogaEdge.TOP, (float) HEADER_H));
        grid.addChild(scroller);

        // ScrollerView owns its scrollable container; children added to the ScrollerView itself are laid
        // out outside the viewport, which is why scrolling had no visible effect.
        UIElement canvas = new UIElement();
        canvas.layout(l -> l.setWidth(GRID_W - 8).setHeight(contentHeight));
        scroller.verticalContainer.addChild(canvas);

        positions.forEach((location, columnRow) -> {
            if (!ClientTreeState.visible(location)) return;
            TreeSnapshot.NodeView node = ClientTreeState.nodes().get(location);
            if (node == null) return;
            int x = Math.max(0, columnRow[0] * (CELL + 6));
            int y = Math.max(0, columnRow[1] * (CELL + 4));
            canvas.addChild(nodePlate(node, x, y));
        });
        return grid;
    }

    private UIElement nodePlate(TreeSnapshot.NodeView node, int x, int y) {
        boolean unlocked = ClientTreeState.unlocked(node.location());
        boolean epic = node.type() == NodeType.EPIC;
        UIElement plate = new UIElement();
        plate.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                .setWidth(CELL).setHeight(CELL)
                .setPosition(YogaEdge.LEFT, (float) x)
                .setPosition(YogaEdge.TOP, (float) y));
        plate.style(s -> s.background(unlocked
                        ? TechTreeTextures.nodePlate(epic, true)
                        : TechTreeTextures.nodePlateDark(epic))
                .overlay(node.logo().isEmpty() ? null
                        : new com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture(node.logo()))
                .tooltips(Component.translatable(node.nameKey()),
                        Component.translatable(node.descriptionKey())));
        plate.addEventListener(UIEvents.CLICK, event -> {
            setMenuFocus(node.location());
            this.init();
            TechTreeRpc.canUnlock().request(node.location().toString())
                    .thenAccept(result -> {
                        ClientTreeState.applyCheck(node.location(), result);
                        Minecraft.getInstance().execute(this::init);
                    });
        });
        return plate;
    }

    /** Bottom bar: unlock confirm button, drawn from the original confirm sprite pair. */
    private UIElement unlockBar() {
        UIElement bar = new UIElement();
        bar.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                .setWidth(16).setHeight(16)
                .setPosition(YogaEdge.LEFT, 168f)
                .setPosition(YogaEdge.TOP, 124f));
        bar.style(s -> s.background(TechTreeTextures.confirm())
                .tooltips(Component.translatable("tech_tree.board.confirm")));
        return bar;
    }

    /** Where the deprecated screen host is bound to a table, so a group choice can be persisted. */
    BlueprintTableBlockEntity table() {
        return table;
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

    /** True when the client can build a board: a player exists and a snapshot has arrived. */
    static boolean ready() {
        return Minecraft.getInstance().player != null && !ClientTreeState.groups().isEmpty();
    }
}
