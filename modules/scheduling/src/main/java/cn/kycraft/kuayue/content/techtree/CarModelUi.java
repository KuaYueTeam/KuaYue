package cn.kycraft.kuayue.content.techtree;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import lib.kasuga.rendering.models.mc.Constants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.network.chat.Component;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaPositionType;
import org.joml.Matrix4f;

/**
 * Blueprint table surface, laid flat on the model through Kasuga's world-UI backend.
 *
 * <p>The element tree lives inside a {@link ModularUI}: text styles and Yoga layout only resolve through the
 * UI that owns them. Drawing the tree directly with a detached context ignored every colour and every
 * position, so all rows came out white and stacked at the origin.</p>
 */
final class CarModelUi implements net.minecraft.client.gui.layouts.LayoutElement,
        net.minecraft.client.gui.components.events.GuiEventListener {

    /** Panel size in GUI pixels. */
    static final int WIDTH = 200;
    /** Compact panel: a tall one overflowed the tabletop and its rows landed on the model. */
    static final int HEIGHT = 96;

    /** Row bands, in panel pixels. */
    private static final float TITLE_TOP = -8f;
    private static final float BODY_TOP = 0f;
    private static final float BUTTON_TOP = 44f;
    private static final float BUTTON_W = 80f;
    private static final float BUTTON_H = 18f;
    private static final float BUTTON_GAP = 90f;

    /** World size of one GUI pixel. */
    private static final float PIXEL = 0.0045f;

    /** Panel centre offset from the table centre, in table-local blocks (X = right, Z = away). */
    private static final float OFFSET_X = 0f;
    private static final float OFFSET_Z = 0f;


    private static final int INK = 0xFFFFFFFF;

    private final ModularUI ui;
    private final UIElement root;
    /** Layout tree is built on first render; rendering before that throws inside taffy. */
    private boolean initialised;

    private CarModelUi() {
        root = new UIElement();
        root.setId("car_model_ui");
        root.layout(l -> l.setWidth(WIDTH).setHeight(HEIGHT));

        root.addChild(label("这是标题", 8f, TITLE_TOP, 0xFFFFF030));
        root.addChild(paragraph("介绍介绍介绍介绍介绍介绍介绍介绍", 8f, BODY_TOP, WIDTH - 16, INK));
        root.addChild(button("按钮A", 8f, BUTTON_TOP));
        root.addChild(button("按钮B", 8f + BUTTON_GAP, BUTTON_TOP));

        ui = ModularUI.of(UI.of(root));
    }

    /** Clickable button; the label is centred over the button box. */
    private static UIElement button(String caption, float x, float y) {
        UIElement holder = new UIElement();
        holder.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                .setPosition(YogaEdge.LEFT, x)
                .setPosition(YogaEdge.TOP, y)
                .setWidth(BUTTON_W).setHeight(BUTTON_H));

        var button = new com.lowdragmc.lowdraglib2.gui.ui.elements.Button();
        button.setText(Component.literal(caption));
        button.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                .setPosition(YogaEdge.LEFT, 0f)
                .setPosition(YogaEdge.TOP, 0f)
                .setWidth(BUTTON_W).setHeight(BUTTON_H));
        button.setOnClick(event -> {
            TechTreeSystem.LOGGER.info("Blueprint table button {} pressed", caption);
            event.stopPropagation();
        });
        holder.addChild(button);
        return holder;
    }

    /**
     * Wrapping paragraph bounded by {@code maxWidth}.
     *
     * <p>Wrapping needs {@code adaptiveWidth(false)} plus an explicit width, otherwise the label sizes itself to
     * the whole string and overflows the panel.</p>
     */
    private static UIElement paragraph(String caption, float x, float y, float maxWidth, int colour) {
        UIElement holder = new UIElement();
        holder.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                .setPosition(YogaEdge.LEFT, x)
                .setPosition(YogaEdge.TOP, y)
                .setWidth(maxWidth).setHeight(HEIGHT - 16));

        Label label = new Label();
        label.setText(Component.literal(caption));
        label.textStyle(s -> s.textColor(colour)
                .adaptiveWidth(false)
                .adaptiveHeight(true)
                .textWrap(com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap.WRAP));
        label.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                .setPosition(YogaEdge.LEFT, 0f)
                .setPosition(YogaEdge.TOP, 0f)
                .setWidth(maxWidth));
        holder.addChild(label);
        return holder;
    }

    /** A label inside a positioned container; the container is what Yoga places. */
    private static UIElement label(String caption, float x, float y, int colour) {
        UIElement holder = new UIElement();
        // Tall enough for a glyph row; a tight box clipped the text away entirely.
        holder.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                .setPosition(YogaEdge.LEFT, x)
                .setPosition(YogaEdge.TOP, y)
                .setWidth(WIDTH - 16).setHeight(24));

        Label label = new Label();
        label.setText(Component.literal(caption));
        label.textStyle(s -> s.textColor(colour).adaptiveWidth(true).adaptiveHeight(true));
        holder.addChild(label);
        return holder;
    }

    static CarModelUi of() {
        return new CarModelUi();
    }

    @Override public void setX(int x) {
    }

    @Override public void setY(int y) {
    }

    @Override public int getX() {
        return 0;
    }

    @Override public int getY() {
        return 0;
    }

    @Override public int getWidth() {
        return WIDTH;
    }

    @Override public int getHeight() {
        return HEIGHT;
    }

    @Override public void visitWidgets(java.util.function.Consumer<net.minecraft.client.gui.components.AbstractWidget> consumer) {
    }

    // Input: the backend only registers an input surface for renderables that are GuiEventListeners, so the
    // events have to be forwarded to the ModularUI widget, which dispatches them to the element tree.

    private boolean focused;

    /**
     * Resolves the duplicate default of {@link LayoutElement} and
     * {@link net.minecraft.client.gui.components.events.GuiEventListener}.
     *
     * <p>Kept at the panel's own frame: the backend uses this rect to place and clip the canvas, so a
     * screen-space rect here made the panel disappear.</p>
     */
    @Override
    public net.minecraft.client.gui.navigation.ScreenRectangle getRectangle() {
        return new net.minecraft.client.gui.navigation.ScreenRectangle(getX(), getY(), WIDTH, HEIGHT);
    }

    @Override public void setFocused(boolean focused) {
        this.focused = focused;
        ui.getWidget().setFocused(focused);
    }

    @Override public boolean isFocused() {
        return focused;
    }

    @Override public void mouseMoved(double mouseX, double mouseY) {
        ui.getWidget().mouseMoved(mouseX, mouseY);
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return ui.getWidget().mouseClicked(mouseX, mouseY, button);
    }

    @Override public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return ui.getWidget().mouseReleased(mouseX, mouseY, button);
    }

    @Override public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return ui.getWidget().mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return ui.getWidget().mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    UIElement root() {
        return root;
    }

    void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!initialised) {
            // Builds the taffy layout tree and the widget; rendering beforehand hits a null node.
            ui.init(WIDTH, HEIGHT);
            initialised = true;
        }
        ui.getWidget().render(graphics, mouseX, mouseY, partialTick);
        // The canvas shares the main buffer source; text batches must be submitted before the framebuffer is
        // read, otherwise every glyph is dropped while rectangles still show.
        graphics.flush();
    }

    void close() {
        ui.onRemoved();
    }

    Renderable renderable() {
        return this::render;
    }

    Renderable attach(Matrix4f localToWorld) {
        Renderable renderable = renderable();
        if (Constants.UI_BACKEND != null) Constants.UI_BACKEND.addRenderable(renderable, localToWorld);
        return renderable;
    }

    /**
     * Flat placement on the table top.
     *
     * <p>Only the backend's own convention is used: local +X right, local +Y down. The XY plane is laid onto the
     * table's XZ plane with the normal up, then mirrored with a 180 turn about the up axis rather than a
     * negative scale, so the surface keeps its outward normal.</p>
     */
    static Matrix4f tableTopTransform(BlueprintTableBlockEntity table) {
        var pos = table.getBlockPos();
        double yaw = Math.toRadians(table.yaw());
        // Offset is applied in the table's own frame, so nudging stays correct at any facing.
        float localZ = OFFSET_Z;
        float dx = (float) (Math.cos(yaw) * OFFSET_X + Math.sin(yaw) * localZ);
        float dz = (float) (-Math.sin(yaw) * OFFSET_X + Math.cos(yaw) * localZ);
        var matrix = new Matrix4f()
                // Well clear of the tabletop: at 0.02 the panel lost the depth test against the table model and
                // was hidden until the block was removed.
                .translate(pos.getX() + 0.5f + dx, pos.getY() + TechTreeBoard.TOP_Y + 0.30f,
                        pos.getZ() + 0.5f + dz)
                .rotateY((float) yaw);
        matrix.rotateY((float) Math.toRadians(180.0f));
        matrix.rotateX((float) Math.toRadians(90.0f));
        matrix.scale(PIXEL, PIXEL, PIXEL);
        matrix.translate(-WIDTH * 0.5f, -HEIGHT * 0.5f, 0f);
        return matrix;
    }
}
