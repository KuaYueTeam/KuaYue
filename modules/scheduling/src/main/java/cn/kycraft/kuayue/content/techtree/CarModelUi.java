package cn.kycraft.kuayue.content.techtree;

import com.lowdragmc.lowdraglib2.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import lib.kasuga.rendering.models.mc.Constants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.network.chat.Component;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaPositionType;

/**
 * Title, description and two buttons on the blueprint table model, built as LDLib2 UI elements.
 *
 * <p>The client requires LDLib2 for this surface, so it stays an element tree mounted through Kasuga's world
 * UI backend. Only these three groups live here: no panel, no background and no model.</p>
 */
final class CarModelUi {

    static final int WIDTH = 200;
    static final int HEIGHT = 200;

    /** Blue area of the model, in canvas pixels. */
    static final int BLUE_X = 10;
    static final int BLUE_Y = 40;
    static final int BLUE_W = 180;
    static final int BLUE_H = 90;

    /** Fixed bands: title line, body block, button row. */
    private static final float TITLE_TOP = 6f;
    private static final float BODY_TOP = 30f;
    private static final float BUTTON_TOP = 132f;

    private static final float LIFT = -4f;

    private static final int INK = 0xFFFFF030;
    private static final int INK_STRONG = 0xFFFFFFFF;

    private final UIElement root = new UIElement();

    private CarModelUi() {
        root.setId("car_model_ui");
        root.layout(l -> l.setWidth(WIDTH).setHeight(HEIGHT));
        root.addChild(blueFrame());
        root.addChild(title());
        root.addChild(body());
        root.addChild(button("按钮A", 0));
        root.addChild(button("按钮B", 1));
    }

    static CarModelUi of() {
        return new CarModelUi();
    }

    UIElement root() {
        return root;
    }

    void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        var context = com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext.of(null, graphics, mouseX, mouseY,
                partialTick);
        root.drawInBackground(context);
        root.drawContents(context);
    }

    void close() {
    }

    /** Red frame around the assumed blue area, for calibration. */
    private static UIElement blueFrame() {
        UIElement frame = new UIElement();
        frame.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                .setPosition(YogaEdge.LEFT, (float) BLUE_X)
                .setPosition(YogaEdge.TOP, (float) BLUE_Y)
                .setWidth(BLUE_W).setHeight(BLUE_H));
        frame.style(s -> s.background(new ColorBorderTexture(1, 0xFFFF3030)));
        return frame;
    }

    private UIElement title() {
        Label label = new Label();
        label.setText(Component.literal("这是标题"));
        label.textStyle(t -> t.textColor(INK_STRONG).adaptiveHeight(true));
        label.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                .setPosition(YogaEdge.LEFT, (float) (BLUE_X + 2))
                .setPosition(YogaEdge.TOP, TITLE_TOP + LIFT)
                .setWidth(BLUE_W - 4));
        return label;
    }

    private UIElement body() {
        Label label = new Label();
        label.setText(Component.literal("这是介绍，介绍介绍介绍介绍介绍介绍介绍介绍"));
        label.textStyle(t -> t.textColor(INK).adaptiveHeight(true));
        label.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                .setPosition(YogaEdge.LEFT, (float) (BLUE_X + 2))
                .setPosition(YogaEdge.TOP, BODY_TOP + LIFT)
                .setWidth(BLUE_W - 12).setHeight(90));
        return label;
    }

    private UIElement button(String caption, int index) {
        Button button = new Button();
        button.setText(Component.literal(caption));
        button.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                .setPosition(YogaEdge.LEFT, (float) (BLUE_X + 8 + index * 88))
                .setPosition(YogaEdge.TOP, BUTTON_TOP + LIFT)
                .setWidth(80).setHeight(18));
        button.setOnClick(event -> {
            TechTreeSystem.LOGGER.info("Blueprint table button {} pressed on table", caption);
            event.stopPropagation();
        });
        return button;
    }

    Renderable renderable() {
        return this::render;
    }

    Renderable attach(org.joml.Matrix4f localToWorld) {
        Renderable renderable = renderable();
        if (Constants.UI_BACKEND != null) Constants.UI_BACKEND.addRenderable(renderable, localToWorld);
        return renderable;
    }

    /**
     * Flat placement on the table top.
     *
     * <p>Laid flat and turned with the table; both scales negative, which is the combination that keeps the
     * plane readable from the front rather than mirrored or upside-down.</p>
     */
    static org.joml.Matrix4f tableTopTransform(BlueprintTableBlockEntity table, float pixelsPerBlock) {
        var pos = table.getBlockPos();
        return new org.joml.Matrix4f()
                .translate(pos.getX() + 0.5f, pos.getY() + TechTreeBoard.TOP_Y + 0.51f, pos.getZ() + 0.5f)
                .rotateY((float) Math.toRadians(table.yaw()))
                .rotateX((float) Math.toRadians(-90.0f))
                .scale(-pixelsPerBlock, -pixelsPerBlock, pixelsPerBlock);
                // No half-size centring: the local origin is the canvas top-left, so element coordinates are
                // used as authored.
    }
}
