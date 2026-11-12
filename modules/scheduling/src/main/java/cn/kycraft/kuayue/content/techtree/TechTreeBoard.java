package cn.kycraft.kuayue.content.techtree;

import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import lib.kasuga.rendering.models.mc.Constants;
import cn.kycraft.kuayue.content.techtree.net.TechTreeRpc;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.appliedenergistics.yoga.YogaAlign;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaFlexDirection;
import org.appliedenergistics.yoga.YogaPositionType;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/**
 * World-space tech tree board drawn on the blueprint table top.
 *
 * <p>The board carries the tree itself: the group strip of the table GUI, one plate per node and the
 * focused node's detail card, all placed in the same 300x150 pixel space as the original
 * {@code blueprint_table.png} sheet. The sheet is deliberately not drawn any more, so the tree is what
 * covers the wooden table model; the container slots of {@link TechTreeTableMenu} sit in the strip below
 * the plates and are painted by {@link TableSlotProjection}.</p>
 *
 * <p>The layout is an LDLib2 element tree registered with KasugaLibNext's world UI backend, which paints it
 * after the level pass at an arbitrary transform and routes mouse input through the same backend.</p>
 */
public final class TechTreeBoard {

    /** Local design space, matching the original blueprint table sheet. */
    public static final int WIDTH = 300;
    public static final int HEIGHT = 150;

    /** Pixels per block for the world-space mapping. */
    public static final float PIXEL = 0.005f;
    /** Height above the block origin where the table top sits. */
    public static final float TOP_Y = 1.01f;

    public static final int HEADER_H = 14;
    public static final int GRID_X = 12;
    public static final int GRID_Y = 16;
    public static final int GRID_W = 276;
    public static final int GRID_H = 106;
    public static final int CELL = 20;


    private static final Map<Long, UIElement> CARDS = new HashMap<>();

    private static BoardRenderable board;
    private static ResourceLocation currentGroup;
    private static boolean pointerOpen;
    /** True while a world board or GUI screen is attached; drives the throttled snapshot refresh. */
    private static volatile boolean uiOpen;

    private TechTreeBoard() {
    }

    /** Bridges the LDLib2 element tree into the {@link Renderable} the world UI backend expects. */
    private static final class BoardRenderable implements Renderable {
        static UIElement root;
        private final UIElement canvas;

        private BoardRenderable() {
            root = new UIElement();
            root.layout(l -> l.setWidth(WIDTH).setHeight(HEIGHT));
            root.style(s -> s.background(TechTreeTextures.boardPaper()));

            this.canvas = new UIElement();
            canvas.layout(l -> l.setWidth(WIDTH).setHeight(HEIGHT)
                    .setPositionType(YogaPositionType.ABSOLUTE));
            root.addChild(canvas);
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            root.layout(l -> l.setWidth(WIDTH).setHeight(HEIGHT));
            GUIContext context = GUIContext.of(null, graphics, mouseX, mouseY, partialTick);
            root.drawInBackground(context);
            root.drawContents(context);
        }
    }

    /**
     * Entry point from the block: fetch the snapshot first, then open the requested host.
     *
     * <p>The client has no tree data until the server answers, so opening the UI directly left an empty
     * board forever. The snapshot request therefore comes first, and the host opens on its completion.</p>
     */
    public static void openFor(BlueprintTableBlockEntity table, boolean worldBoard) {
        if (table == null) return;
        if (!ClientTreeState.groups().isEmpty()) {
            if (worldBoard) open(table); else TechTreeScreen.open(table);
            return;
        }
        TechTreeRpc.open().run(new TechTreeRpc.OpenPayload(false)).whenComplete((snapshot, error) -> {
            if (error != null) {
                TechTreeSystem.LOGGER.warn("Tech tree snapshot request failed", error);
                return;
            }
            ClientTreeState.apply(snapshot);
            uiOpen = true;
            net.minecraft.client.Minecraft.getInstance().execute(() -> {
                if (worldBoard) open(table); else TechTreeScreen.open(table);
            });
        });
    }

    /** Builds (or focuses) the board for a table and shows the world pointer. Client thread only. */
    public static void open(BlueprintTableBlockEntity table) {
        if (Constants.UI_BACKEND == null) return;
        ResourceLocation group = resolveGroup(table.selectedGroup());
        if (board == null) {
            board = new BoardRenderable();
        }
        if (group != null && !group.equals(currentGroup)) {
            currentGroup = group;
            rebuild(group);
        }
        uiOpen = true;
        Constants.UI_BACKEND.addRenderable(board, tableTransform(table, null));
        // No openPointer() here: it installs a WorldUiInputScreen that swallows mouse look and every later
        // right-click, which left the table unusable after the first use.
    }

    /** Binds by position only; used by the container-menu path where the block entity is not at hand. */
    static void bindPosition(net.minecraft.world.entity.player.Player player, net.minecraft.core.BlockPos pos) {
        if (player == null || player.level() == null) return;
        if (player.level().getBlockEntity(pos) instanceof BlueprintTableBlockEntity table) {
            ResourceLocation group = resolveGroup(table.selectedGroup());
            if (group != null) currentGroup = group;
            uiOpen = true;
        }
    }

    /** Binds the board to a table without registering a world renderable (GUI host). */
    public static void bind(BlueprintTableBlockEntity table) {
        ResourceLocation group = resolveGroup(table.selectedGroup());
        if (group != null && !group.equals(currentGroup)) {
            currentGroup = group;
            if (board != null) rebuild(group);
        }
    }

    /** Builds the shared element tree; used by the GUI host. */
    public static UIElement createElementTree() {
        if (board == null) {
            board = new BoardRenderable();
        }
        return board.root;
    }

    /**
     * Board content for a GUI host.
     *
     * <p>Returns a fresh tree instead of the world board's root: a UI element has one parent, so handing the
     * live board element to a menu UI would steal it from the world scene. The content is the same tree the
     * world board paints, rebuilt against the current snapshot.</p>
     */
    /** Light-blue drawing field of {@code blueprint_table.png}, in sheet pixels. */
    static final int FIELD_X = 4;
    static final int FIELD_Y = 8;
    static final int FIELD_W = 288;
    static final int FIELD_H = 121;

    /**
     * Board content sized to the sheet's blue drawing field.
     *
     * <p>The sheet already draws the outer frame, the title strip and the slot tray, so the tree is laid out
     * inside that field rather than over the whole 300x150 panel.</p>
     */
    public static UIElement buildGuiBoard() {
        UIElement canvas = new UIElement();
        canvas.setId("tech_tree_board");
        canvas.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                .setPosition(YogaEdge.LEFT, (float) FIELD_X)
                .setPosition(YogaEdge.TOP, (float) FIELD_Y)
                .setWidth(FIELD_W).setHeight(FIELD_H));
        fillCanvas(canvas, resolveGroup(null));
        return canvas;
    }

    /** True when a board or screen is currently attached to a table. */
    public static boolean isBound() {
        return board != null;
    }

    /** True while either host is showing; the client tick refresh keys off this, not on {@link #isBound()}. */
    public static boolean isUiOpen() {
        return uiOpen;
    }

    public static void markUiClosed() {
        uiOpen = false;
    }

    public static void close() {
        uiOpen = false;
        if (board != null && Constants.UI_BACKEND != null) {
            Constants.UI_BACKEND.removeRenderable(board);
        }
        pointerOpen = false;
    }

    /** Rebuilds the board after a snapshot refresh; called from the client tick while the board is up. */
    static void refresh() {
        ResourceLocation group = currentGroup != null ? currentGroup : ClientTreeState.selectedGroup();
        if (group != null) {
            currentGroup = group;
            rebuild(group);
        }
    }

    /**
     * Local pixel space -> world, lying flat on the table top.
     *
     * <p>{@code view} is the model view matrix the caller renders with; passing {@code null} yields the
     * transform relative to the camera, which is what the world UI backend expects.</p>
     */
    public static Matrix4f tableTransform(BlueprintTableBlockEntity table, Matrix4f view) {
        var pos = table.getBlockPos();
        Matrix4f matrix = new Matrix4f();
        if (view != null) matrix.mul(view);
        return matrix
                .translate(pos.getX() + 0.5f, pos.getY() + TOP_Y, pos.getZ() + 0.5f)
                .rotateY((float) Math.toRadians(table.yaw()))
                .rotateX((float) Math.toRadians(-90.0f))
                .scale(PIXEL, -PIXEL, 1.0f)
                .translate(-WIDTH * 0.5f, -HEIGHT * 0.5f, 0);
    }

    private static void rebuild(ResourceLocation groupId) {
        if (board == null) return;
        fillCanvas(board.canvas, groupId);
    }

    /**
     * Fills a canvas with the current tree.
     *
     * <p>Separate from {@link #rebuild} because the GUI needs its own element tree: a UI element belongs to a
     * single parent, so the world board's canvas cannot be mounted into a menu UI - doing that moved it out of
     * the world and left both hosts empty.</p>
     */
    private static void fillCanvas(UIElement canvas, ResourceLocation groupId) {
        for (UIElement child : new java.util.ArrayList<>(canvas.getChildren())) {
            canvas.removeChild(child);
        }
        CARDS.clear();

        mapPin(groupId);
        canvas.addChild(groupStrip());

        TreeSnapshot.GroupView view = ClientTreeState.groups().get(groupId);
        if (view != null) {
            Label title = new Label();
            title.setText(Component.translatable(view.nameKey()));
            title.textStyle(t -> t.textColor(0xFF2A2018).adaptiveHeight(true));
            title.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                    .setPosition(YogaEdge.LEFT, (float) (GRID_X + 2))
                    .setPosition(YogaEdge.TOP, 3f));
            canvas.addChild(title);
        }

        Map<NodeLocation, int[]> positions = ClientTreeState.layout(groupId);
        positions.forEach((location, columnRow) -> {
            if (!ClientTreeState.visible(location)) return;
            TreeSnapshot.NodeView node = ClientTreeState.nodes().get(location);
            if (node == null) return;
            canvas.addChild(nodePlate(node, GRID_X + Math.max(0, columnRow[0]) * (CELL + 6),
                    GRID_Y + Math.max(0, columnRow[1]) * (CELL + 4)));
        });

        NodeLocation focused = TechTreeScreen.getMenuFocus();
        if (focused != null && ClientTreeState.nodes().containsKey(focused)) {
            canvas.addChild(TechTreeDetailPanel.build(ClientTreeState.nodes().get(focused),
                    ClientTreeState.unlocked(focused)));
        }
    }

    /** Left column of the board: one button per published group, above the plate grid. */
    private static UIElement groupStrip() {
        UIElement strip = new UIElement();
        strip.layout(l -> l.setWidth(10).setHeight(GRID_H)
                .setPositionType(YogaPositionType.ABSOLUTE)
                .setPosition(YogaEdge.LEFT, 1f)
                .setPosition(YogaEdge.TOP, (float) GRID_Y)
                .setFlexDirection(YogaFlexDirection.COLUMN));
        int index = 0;
        for (ResourceLocation id : ClientTreeState.groups().keySet()) {
            if (index++ >= 6) break;
            TreeSnapshot.GroupView group = ClientTreeState.groups().get(id);
            UIElement button = new UIElement();
            button.layout(l -> l.setWidth(9).setHeight(9));
            button.style(s -> s.background(TechTreeTextures.groupButton())
                    .overlay(group.icon().isEmpty() ? null : new ItemStackTexture(group.icon())));
            button.addEventListener(UIEvents.CLICK, event -> {
                ClientTreeState.selectGroup(id);
                currentGroup = id;
                TechTreeRpc.open().run(new TechTreeRpc.OpenPayload(false))
                        .thenAccept(ClientTreeState::apply);
                rebuild(id);
            });
            strip.addChild(button);
        }
        return strip;
    }

    private static UIElement nodePlate(TreeSnapshot.NodeView node, int x, int y) {
        boolean unlocked = ClientTreeState.unlocked(node.location());
        UIElement plate = new UIElement();
        plate.setId("node_" + node.location());
        plate.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                .setWidth(CELL).setHeight(CELL)
                .setPosition(YogaEdge.LEFT, (float) x)
                .setPosition(YogaEdge.TOP, (float) y));
        plate.style(s -> s.background(unlocked
                        ? TechTreeTextures.nodePlate(node.type() == NodeType.EPIC, true)
                        : TechTreeTextures.nodePlateDark(node.type() == NodeType.EPIC))
                .overlay(node.logo().isEmpty() ? null : new ItemStackTexture(node.logo()))
                .tooltips(Component.translatable(node.nameKey()),
                        Component.translatable(node.descriptionKey())));

        plate.addEventListener(UIEvents.CLICK, event -> {
            TechTreeScreen.setMenuFocus(node.location());
            rebuild(currentGroup);
            TechTreeRpc.canUnlock().request(node.location().toString())
                    .thenAccept(result -> {
                        ClientTreeState.applyCheck(node.location(), result);
                        net.minecraft.client.Minecraft.getInstance().execute(() -> {
                            if (currentGroup != null) rebuild(currentGroup);
                        });
                    });
        });
        return plate;
    }

    /** The board used the detail pane of the screen at the same size; keep the card geometry aligned. */
    private static void mapPin(ResourceLocation groupId) {
        CARDS.clear();
    }

    private static ResourceLocation resolveGroup(String raw) {
        if (raw != null && !raw.isEmpty()) {
            ResourceLocation parsed = ResourceLocation.tryParse(raw);
            if (parsed != null && ClientTreeState.groups().containsKey(parsed)) return parsed;
        }
        return ClientTreeState.selectedGroup();
    }

}
