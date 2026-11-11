package cn.kycraft.kuayue.content.techtree;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import lib.kasuga.rendering.models.mc.Constants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaPositionType;

/**
 * Text column that stands beside the blueprint model on a table.
 *
 * <p>The model itself is drawn in the world by Kasuga's scene; this host carries only the title and the
 * description, upright next to the table. It stays its own element tree so it can be nested into another
 * layout later.</p>
 */
final class CarModelUi {

    /** Width of the plate the model occupies, in GUI pixels. */
    static final int SCENE_SIZE = 150;
    /** Width of the whole host: the model column plus the text column. */
    static final int WIDTH = 300;
    static final int HEIGHT = 150;

    /** Model column starts at the left edge. */
    private static final int SCENE_X = 0;
    private static final int SCENE_Y = 0;

    /** Text starts right after the model column. */
    private static final float TEXT_X = SCENE_X + SCENE_SIZE + 10;
    private static final float TEXT_W = WIDTH - TEXT_X - 4;

    // Light ink: the text is drawn over the world now, not over dark parchment.
    private static final int INK = 0xFFE8E2D8;
    private static final int INK_STRONG = 0xFFFFF6E2;

    private final UIElement root = new UIElement();
    private final CarModelScene scene = new CarModelScene(SCENE_SIZE, SCENE_SIZE);
    private final ResourceLocation blueprint;

    private CarModelUi(ResourceLocation blueprint) {
        this.blueprint = blueprint;
        root.setId("car_model_ui");
        root.layout(l -> l.setWidth(WIDTH).setHeight(HEIGHT));

        UIElement plate = scene.element();
        plate.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                .setPosition(YogaEdge.LEFT, SCENE_X)
                .setPosition(YogaEdge.TOP, SCENE_Y)
                .setWidth(SCENE_SIZE).setHeight(SCENE_SIZE));
        root.addChild(plate);
        root.addChild(title());
        root.addChild(body());
    }

    static CarModelUi of(ResourceLocation blueprint) {
        return new CarModelUi(blueprint);
    }

    UIElement root() {
        return root;
    }

    CarModelScene scene() {
        return scene;
    }

    ResourceLocation blueprint() {
        return blueprint;
    }

    /** Draws only the text column; the model is drawn in the world. */
    void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        var context = com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext.of(null, graphics, mouseX, mouseY,
                partialTick);
        root.drawInBackground(context);
        root.drawContents(context);
    }

    void close() {
    }

    private UIElement title() {
        Label label = new Label();
        label.setText(Component.translatable("tech_tree.blueprint.title"));
        label.textStyle(t -> t.textColor(INK_STRONG).adaptiveHeight(true));
        label.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                .setPosition(YogaEdge.LEFT, TEXT_X)
                .setPosition(YogaEdge.TOP, 40f)
                .setWidth(TEXT_W));
        return label;
    }

    private UIElement body() {
        Label label = new Label();
        label.setText(Component.translatable("tech_tree.blueprint.description"));
        label.textStyle(t -> t.textColor(INK).adaptiveHeight(true));
        label.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                .setPosition(YogaEdge.LEFT, TEXT_X)
                .setPosition(YogaEdge.TOP, 64f)
                .setWidth(TEXT_W));
        return label;
    }

    Renderable renderable() {
        return this::render;
    }

    /** Registers the text column on the table model at the given transform. */
    Renderable attach(org.joml.Matrix4f localToWorld) {
        Renderable renderable = renderable();
        if (Constants.UI_BACKEND != null) Constants.UI_BACKEND.addRenderable(renderable, localToWorld);
        return renderable;
    }

    /**
     * Upright placement of the host above the table top.
     *
     * <p>Anchored at the bottom-centre of the whole host, not of the model plate: anchoring on the plate
     * pushed the text column far outside the table.</p>
     */
    static org.joml.Matrix4f uprightTransform(BlueprintTableBlockEntity table, float pixelsPerBlock) {
        var pos = table.getBlockPos();
        float anchorX = WIDTH * 0.5f;
        float anchorY = HEIGHT;
        return new org.joml.Matrix4f()
                .translate(pos.getX() + 0.5f, pos.getY() + TechTreeBoard.TOP_Y + 0.5f, pos.getZ() + 0.5f)
                .rotateY((float) Math.toRadians(table.yaw()))
                // GUI coordinates have Y down and the plate is rotated onto a horizontal surface, so both
                // axes are negated; without the X flip every glyph reads backwards.
                .scale(-pixelsPerBlock, -pixelsPerBlock, pixelsPerBlock)
                .translate(-anchorX, -anchorY, 0f);
    }
}
