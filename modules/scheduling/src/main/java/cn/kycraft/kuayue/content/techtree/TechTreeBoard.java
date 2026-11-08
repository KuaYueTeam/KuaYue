package cn.kycraft.kuayue.content.techtree;

import com.lowdragmc.lowdraglib2.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import lib.kasuga.rendering.models.mc.Constants;
import cn.kycraft.kuayue.content.techtree.net.TechTreeRpc;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * World-space tech tree board drawn on the blueprint table top.
 *
 * <p>Layout is a LDLib2 (taffy/yoga) element tree; the tree is registered with KasugaLibNext's world UI
 * backend, which paints it into the world after the level pass at an arbitrary transform. Mouse input
 * arrives through the same backend, so the board behaves like a physical panel instead of a screen.</p>
 */
public final class TechTreeBoard {

    public static final int WIDTH = 512;
    public static final int HEIGHT = 320;
    private static final int CELL_W = 96;
    private static final int CELL_H = 64;

    private static final Map<ResourceLocation, List<UIElement>> CARDS = new HashMap<>();

    private static BoardRenderable board;
    private static ResourceLocation currentGroup;
    private static boolean pointerOpen;
    /** True while a world board or GUI screen is attached; drives the throttled snapshot refresh. */
    private static volatile boolean uiOpen;

    private TechTreeBoard() {
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
        Constants.UI_BACKEND.addRenderable(board, transformFor(table));
        if (!pointerOpen) {
            pointerOpen = true;
            Constants.UI_BACKEND.openPointer();
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

    /** Local pixel space -> world, lying flat on the table top. */
    private static Matrix4f transformFor(BlueprintTableBlockEntity table) {
        var pos = table.getBlockPos();
        return new Matrix4f()
                .translate(pos.getX() + 0.5f, pos.getY() + 1.005f, pos.getZ() + 0.5f)
                .rotateY((float) Math.toRadians(table.yaw()))
                .rotateX((float) Math.toRadians(-90.0f))
                .scale(0.005f, -0.005f, 1.0f);
    }

    private static void rebuild(ResourceLocation groupId) {
        if (board == null) return;
        UIElement canvas = board.canvas;
        for (UIElement child : new java.util.ArrayList<>(canvas.getChildren())) {
            canvas.removeChild(child);
        }
        CARDS.clear();

        Map<NodeLocation, int[]> positions = ClientTreeState.layout(groupId);
        Map<Long, UIElement> columns = new HashMap<>();
        positions.forEach((location, columnRow) -> {
            if (!ClientTreeState.visible(location)) return;
            TreeSnapshot.NodeView node = ClientTreeState.nodes().get(location);
            if (node == null) return;
            UIElement column = columns.computeIfAbsent((long) columnRow[0], col -> {
                UIElement element = new UIElement();
                element.layout(l -> l.setWidth(CELL_W)
                        .setFlexDirection(org.appliedenergistics.yoga.YogaFlexDirection.COLUMN));
                canvas.addChild(element);
                return element;
            });
            column.addChild(nodeCard(node));
        });
    }

    private static UIElement nodeCard(TreeSnapshot.NodeView node) {
        boolean unlocked = ClientTreeState.unlocked(node.location());
        UIElement card = new UIElement();
        card.layout(l -> l.setWidth(CELL_W - 12).setHeight(CELL_H)
                .setFlexDirection(org.appliedenergistics.yoga.YogaFlexDirection.ROW)
                .setAlignItems(org.appliedenergistics.yoga.YogaAlign.CENTER));
        card.style(s -> s.background(TechTreeTextures.nodePlate(node.type() == NodeType.EPIC, unlocked))
                .overlay(TechTreeTextures.outline(unlocked ? 0xFF63C07A : 0xFF4A5866))
                .tooltips(Component.translatable(node.nameKey()),
                        Component.translatable(node.descriptionKey())));

        ItemStack logo = node.logo();
        if (!logo.isEmpty()) {
            UIElement icon = new UIElement();
            icon.layout(l -> l.setWidth(20).setHeight(20));
            icon.style(s -> s.background(new ItemStackTexture(logo)));
            card.addChild(icon);
        }

        Label name = new Label();
        name.setText(Component.translatable(node.nameKey()));
        name.textStyle(t -> t.textColor(unlocked ? 0xFFB9F0C4 : 0xFFDCE3EA).adaptiveHeight(true));
        name.layout(l -> l.setWidthAuto().setMinWidth(CELL_W - 44));
        card.addChild(name);

        card.addEventListener(UIEvents.CLICK, event -> TechTreeRpc.unlockNode()
                .request(node.location().toString())
                .thenAccept(ClientTreeState::apply));
        return card;
    }

    private static ResourceLocation resolveGroup(String raw) {
        if (raw != null && !raw.isEmpty()) {
            ResourceLocation parsed = ResourceLocation.tryParse(raw);
            if (parsed != null && ClientTreeState.groups().containsKey(parsed)) return parsed;
        }
        return ClientTreeState.selectedGroup();
    }

    /** Bridges the LDLib2 element tree into the {@link Renderable} the world UI backend expects. */
    private static final class BoardRenderable implements Renderable {
        static UIElement root;
        private final UIElement canvas;

        private BoardRenderable() {
            root = new UIElement();
            root.layout(l -> l.setWidth(WIDTH).setHeight(HEIGHT));
            root.style(s -> s.background(TechTreeTextures.board()));

            Label title = new Label();
            title.setText(Component.translatable("tech_tree.board.title"));
            title.textStyle(t -> t.textColor(0xFFFFFFFF).adaptiveHeight(true));
            title.layout(l -> l.setPositionType(org.appliedenergistics.yoga.YogaPositionType.ABSOLUTE)
                    .setWidth(WIDTH - 24));
            root.addChild(title);

            this.canvas = new UIElement();
            canvas.layout(l -> l.setWidth(WIDTH).setHeight(HEIGHT - 40)
                    .setFlexDirection(org.appliedenergistics.yoga.YogaFlexDirection.ROW)
                    .setAlignItems(org.appliedenergistics.yoga.YogaAlign.FLEX_START));
            root.addChild(canvas);
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            root.layout(l -> { l.setWidth(WIDTH).setHeight(HEIGHT); });
            GUIContext context = GUIContext.of(null, graphics, mouseX, mouseY, partialTick);
            root.drawInBackground(context);
            root.drawContents(context);
        }
    }
}
