package cn.kycraft.kuayue.content.techtree;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import cn.kycraft.kuayue.content.techtree.net.TechTreeRpc;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

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
    private static final int HEADER_H = 14;
    /** Height reserved for the player inventory that the container menu renders below the board. */
    private static final int INVENTORY_H = 90;
    private static final int SLOT_STEP = 18;
    private static final int SLOT_X = 8;
    private static final int SLOT_Y = 6;

    private final BlueprintTableBlockEntity table;
    private ResourceLocation group;
    private NodeLocation focused;
    private UIElement detail;

    /**
     * Entry point for the container-backed screen opened by {@link TechTreeMenuHost}.
     *
     * <p>The screen here is driven by the {@code ModularUIContainerMenu} that LDLib2 creates from this
     * ModularUI, which is what gives the item slots a real backing {@code Slot} and shows the player
     * inventory underneath, like a chest.</p>
     */
    static ModularUI createModularUI(net.minecraft.world.entity.player.Player player,
                                     net.minecraft.core.BlockPos pos) {
        TechTreeBoard.bindPosition(player, pos);
        ModularUI ui = ModularUI.of(UI.of(buildBoardRoot()), player);
        if (player.level().isClientSide) {
            TechTreeRpc.open().run(new TechTreeRpc.OpenPayload(player.isCreative()))
                    .thenAccept(snapshot -> {
                        ClientTreeState.apply(snapshot);
                        // The board was painted from an empty cache on first open; repaint now.
                        net.minecraft.client.Minecraft.getInstance().execute(TechTreeScreen::rebuildOpenMenuBoard);
                    });
        }
        return ui;
    }

    /** Board container of the menu-hosted UI, so an async refresh can repaint it. */
    private static volatile UIElement openMenuBoard;

    /**
     * Fallback flush driven from the client tick.
     *
     * <p>The RPC callback fires once; if it lands before {@code ModularUI.setMenu}, the slots would stay
     * unregistered. Ticking retries until the menu actually has them.</p>
     */
    static void tickPendingInventorySlots() {
        if (!PENDING_INVENTORY_SLOTS.isEmpty()) {
            flushPendingInventorySlots();
        }
    }

    /** Repaints the currently open menu board from the latest client state. */
    static void rebuildOpenMenuBoard() {
        UIElement board = openMenuBoard;
        if (board != null && board.getParent() != null) {
            fillBoard(board);
        }
        flushPendingInventorySlots();
    }

    /** Element tree of the container-backed screen; rebuilt in place when the focused node changes. */
    static UIElement buildBoardRoot() {
        UIElement root = new UIElement();
        root.layout(l -> l.setWidth(BOARD_W).setHeight(BOARD_H + INVENTORY_H));
        root.style(s -> s.background(TechTreeTextures.board()));

        UIElement board = new UIElement();
        board.setId("tech_tree_board");
        board.layout(l -> l.setWidth(BOARD_W).setHeight(BOARD_H));
        board.style(s -> s.background(TechTreeTextures.board()));
        root.addChild(board);
        openMenuBoard = board;

        fillBoard(board);
        root.addChild(buildPlayerInventory());
        return root;
    }

    /**
     * Player inventory below the board.
     *
     * <p>LDLib2's {@code ModularUIContainerMenu} only stores the {@code Inventory}; it neither adds the
     * 36 vanilla slots nor draws a background. Each slot therefore gets a real {@link ItemSlot} bound to
     * the player inventory. Binding before the menu is attached is fine: {@code ItemSlot} re-registers
     * itself from {@code onModularUIChanged} once {@code ModularUI.setMenu} runs.</p>
     */
    static UIElement buildPlayerInventory() {
        UIElement area = new UIElement();
        area.setId("tech_tree_inventory");
        area.layout(l -> l.setPositionType(org.appliedenergistics.yoga.YogaPositionType.ABSOLUTE)
                .setWidth(BOARD_W).setHeight(INVENTORY_H)
                .setPosition(org.appliedenergistics.yoga.YogaEdge.TOP, (float) BOARD_H));
        // Faint backing only; each slot draws the vanilla slot sprite itself.
        area.style(s -> s.background(TechTreeTextures.inventoryBg()));

        var player = net.minecraft.client.Minecraft.getInstance().player;
        var inventory = player == null ? null : player.getInventory();
        for (int i = 0; i < 36; i++) {
            int index = i;
            ItemSlot slot = new ItemSlot();
            // Vanilla slot sprite: without it the cell is just the flat backing colour.
            slot.style(st -> st.background(ItemSlot.ITEM_SLOT_TEXTURE));
            slot.layout(l -> l.setPositionType(org.appliedenergistics.yoga.YogaPositionType.ABSOLUTE)
                    .setWidth(18).setHeight(18)
                    .setPosition(org.appliedenergistics.yoga.YogaEdge.LEFT, (float) (SLOT_X + (index % 9) * SLOT_STEP))
                    .setPosition(org.appliedenergistics.yoga.YogaEdge.TOP,
                            (float) (SLOT_Y + (index / 9) * SLOT_STEP)));
            if (inventory != null) {
                PENDING_INVENTORY_SLOTS.add(new PendingSlot(slot, new net.minecraft.world.inventory.Slot(inventory, index, 0, 0)));
            }
            area.addChild(slot);
        }
        return area;
    }

    /** A slot plus its backing vanilla Slot, bound once the container menu actually exists. */
    private record PendingSlot(ItemSlot element, net.minecraft.world.inventory.Slot backing) {
    }

    /**
     * Slots created before the menu exists.
     *
     * <p>{@code ItemSlot.bind} snapshots the Slot and registers it through {@code ModularUI.getMenu()}.
     * During {@code BlockUI.createUI} that menu is still null, so the vanilla Slot never reaches
     * {@code AbstractContainerMenu.slots}; the server then rejects every pickup and the stack snaps back
     * ("item disappears on click"). The bindings are therefore deferred until the menu is set.</p>
     */
    private static final java.util.List<PendingSlot> PENDING_INVENTORY_SLOTS =
            java.util.Collections.synchronizedList(new java.util.ArrayList<>());

    /** Binds the deferred inventory slots; safe to call repeatedly. */
    static void flushPendingInventorySlots() {
        synchronized (PENDING_INVENTORY_SLOTS) {
            for (PendingSlot pending : PENDING_INVENTORY_SLOTS) {
                // bind(null) first: bind(slot) is a no-op when the same Slot is already stored.
                // Cast disambiguates ItemSlot.bind(Slot) from IBindable.bind(IBinding).
                pending.element().bind((net.minecraft.world.inventory.Slot) null);
                pending.element().bind(pending.backing());
            }
            PENDING_INVENTORY_SLOTS.clear();
        }
    }

    /**
     * Paints either the tree grid or the node detail pane into {@code board}, mirroring the screen layout:
     * the two views are mutually exclusive because the detail pane covers the whole board.
     */
    static void fillBoard(UIElement board) {
        for (UIElement child : new java.util.ArrayList<>(board.getChildren())) {
            board.removeChild(child);
        }

        ResourceLocation group = ClientTreeState.selectedGroup();
        if (group != null) {
            ClientTreeState.selectGroup(group);
        }

        NodeLocation focused = MENU_FOCUS.get();
        boolean detailView = focused != null && ClientTreeState.nodes().containsKey(focused);

        TechTreeScreen builder = new TechTreeScreen(null);
        builder.group = group;
        builder.focused = focused;

        if (detailView) {
            board.addChild(TechTreeDetailPanel.build(ClientTreeState.nodes().get(focused),
                    ClientTreeState.unlocked(focused)));
        } else {
            board.addChild(builder.treeGrid());
        }
    }

    /** Focused node of the container-backed screen; menu UIs are rebuilt through {@link #fillBoard}. */
    private static final java.util.function.Supplier<NodeLocation> MENU_FOCUS =
            new java.util.function.Supplier<>() {
                private NodeLocation value;

                @Override
                public NodeLocation get() {
                    return value;
                }
            };

    static void setMenuFocus(NodeLocation focus) {
        try {
            java.lang.reflect.Field f = MENU_FOCUS.getClass().getDeclaredField("value");
            f.setAccessible(true);
            f.set(MENU_FOCUS, focus);
        } catch (ReflectiveOperationException ignored) {
            // Focus is cosmetic; the tree still renders if it cannot be stored.
        }
    }

    static NodeLocation getMenuFocus() {
        return MENU_FOCUS.get();
    }

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

    /**
     * Rebuilds the board inside a container menu.
     *
     * <p>A menu-hosted {@code ModularUI} has no {@code Screen} to re-init, so the element tree is edited in
     * place: the board container is repopulated with the other view.</p>
     */
    private static void rebuildMenuBoard(com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent event) {
        UIElement cursor = event != null && event.currentElement != null ? event.currentElement : null;
        UIElement board = cursor;
        while (board != null && !"tech_tree_board".equals(board.getId())) {
            board = board.getParent();
        }
        if (board != null) {
            fillBoard(board);
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
        arrow.style(style -> style.background(TechTreeTextures.progressArrow(progress)));
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
        scroller.layout(l -> l.setPositionType(org.appliedenergistics.yoga.YogaPositionType.ABSOLUTE)
                .setWidth(GRID_W).setHeight(viewHeight)
                .setPosition(org.appliedenergistics.yoga.YogaEdge.TOP, (float) HEADER_H));
        grid.addChild(scroller);

        // ScrollerView owns its own scrollable container; children added to the ScrollerView itself are
        // laid out outside the viewport, which is why scrolling had no visible effect.
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
            setMenuFocus(node.location());
            if (table != null) {
                // Screen host: rebuild widgets directly.
                this.init();
            } else {
                rebuildMenuBoard(event);
            }
            TechTreeRpc.canUnlock().request(node.location().toString())
                    .thenAccept(result -> {
                        ClientTreeState.applyCheck(node.location(), result);
                        net.minecraft.client.Minecraft.getInstance().execute(() -> {
                            if (table != null) this.init();
                            else rebuildMenuBoard(null);
                        });
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
        bar.style(s -> s.background(TechTreeTextures.confirm())
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
