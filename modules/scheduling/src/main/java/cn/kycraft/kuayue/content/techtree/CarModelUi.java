package cn.kycraft.kuayue.content.techtree;

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
 * Title, description and two buttons drawn on the blueprint table model.
 *
 * <p>Only these three elements live here: no panel, no background and no model. The model is a world scene
 * rendered by Kasuga, so anything drawn by this host would sit on top of it as a separate surface.</p>
 */
final class CarModelUi {

    /** Drawing area in local pixels, matching the blue part of the table model. */
    static final int WIDTH = 200;
    static final int HEIGHT = 200;

    /** Blue area of the model, in this canvas. */
    static final int BLUE_X = 10;
    static final int BLUE_Y = 40;
    static final int BLUE_W = 180;
    static final int BLUE_H = 90;

    /** Every element is lifted by this many pixels. */
    private static final float LIFT = -4f;

    private static final int INK = 0xFF1E2A44;
    private static final int INK_STRONG = 0xFF10203A;

    private final UIElement root = new UIElement();

    private CarModelUi() {
        root.setId("car_model_ui");
        root.layout(l -> l.setWidth(WIDTH).setHeight(HEIGHT));
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

    /** Title above the blue area. */
    private UIElement title() {
        Label label = new Label();
        label.setText(Component.literal("这是标题"));
        label.textStyle(t -> t.textColor(INK_STRONG).adaptiveHeight(true));
        label.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                .setPosition(YogaEdge.LEFT, (float) (BLUE_X + 2))
                .setPosition(YogaEdge.TOP, (float) (BLUE_Y - 16) + LIFT)
                .setWidth(BLUE_W - 4));
        return label;
    }

    /** Description inside the blue area; the label wraps at its width. */
    private UIElement body() {
        Label label = new Label();
        label.setText(Component.literal("这是介绍，介绍介绍介绍介绍介绍介绍介绍介绍"));
        label.textStyle(t -> t.textColor(INK).adaptiveHeight(true));
        label.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                .setPosition(YogaEdge.LEFT, (float) (BLUE_X + 6))
                .setPosition(YogaEdge.TOP, (float) (BLUE_Y + 6) + LIFT)
                .setWidth(BLUE_W - 12));
        return label;
    }

    /** Clickable button below the blue area. */
    private UIElement button(String caption, int index) {
        Button button = new Button();
        button.setText(Component.literal(caption));
        button.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                .setPosition(YogaEdge.LEFT, (float) (BLUE_X + 8 + index * 88))
                .setPosition(YogaEdge.TOP, (float) (BLUE_Y + BLUE_H + 6) + LIFT)
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

    /** Registers the overlay on the table model at the given transform. */
    Renderable attach(org.joml.Matrix4f localToWorld) {
        Renderable renderable = renderable();
        if (Constants.UI_BACKEND != null) Constants.UI_BACKEND.addRenderable(renderable, localToWorld);
        return renderable;
    }

    /**
     * Flat placement on the table top.
     *
     * <p>Shares the table's horizontal plane. X is negated because the surface is rotated onto a horizontal
     * top, otherwise the glyphs read backwards.</p>
     */
    static org.joml.Matrix4f tableTopTransform(BlueprintTableBlockEntity table, float pixelsPerBlock) {
        var pos = table.getBlockPos();
        // Same seat as the model (its own left offset and height), so the overlay lands on the blue part of
        // the table top instead of over the block centre.
        var seat = BlueprintBlockRenderer.seatOf(pos, table.yaw());
        return new org.joml.Matrix4f()
                // Sit just above the table top, not half a block up: at +0.5 the plate floated above the
                // surface and had to be viewed from below.
                .translate((float) seat.x, (float) seat.y + 0.01f, (float) seat.z)
                .rotateY((float) Math.toRadians(table.yaw()))
                .rotateX((float) Math.toRadians(-90.0f))
                // GUI space has Y down; on a horizontal top that also mirrors X, so X must not be negated as
                // well or every glyph reads backwards.
                .scale(pixelsPerBlock, -pixelsPerBlock, pixelsPerBlock)
                .translate(-WIDTH * 0.5f, -HEIGHT * 0.5f, 0f);
    }
}
