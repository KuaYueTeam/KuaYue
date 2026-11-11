package cn.kycraft.kuayue.content.techtree;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import lib.kasuga.rendering.models.mc.Constants;
import lib.kasuga.rendering.models.mc.backend.GuiModelPreview;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaPositionType;

/**
 * Standalone host of a {@link CarModelScene}: the model on the left, title and text on the right.
 *
 * <p>This is its own UI, not part of the tech tree. It is built so it can be embedded later: {@link #root()}
 * is a plain element tree, and the scene element inside it can be nested into any other layout.</p>
 */
final class CarModelUi {

    /** Board size in GUI pixels; matches the blueprint sheet so both hosts can share a transform. */
    static final int WIDTH = 300;
    static final int HEIGHT = 150;

    private static final int SCENE_X = 14;
    private static final int SCENE_Y = 0;
    private static final int SCENE_SIZE = 150;

    private static final float TEXT_X = SCENE_X + SCENE_SIZE + 14;
    private static final float TEXT_W = 108;

    private static final int INK = 0xFF4A3B2A;
    private static final int INK_STRONG = 0xFF2A2018;

    private final UIElement root = new UIElement();
    private final CarModelScene scene = new CarModelScene(SCENE_SIZE, SCENE_SIZE);
    private final ResourceLocation blueprint;

    private CarModelUi(ResourceLocation blueprint) {
        this.blueprint = blueprint;
        root.setId("car_model_ui");
        root.layout(l -> l.setWidth(WIDTH).setHeight(HEIGHT));
        root.style(s -> s.background(TechTreeTextures.boardPaper()));

        UIElement plate = scene.element();
        plate.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                .setPosition(YogaEdge.LEFT, SCENE_X)
                .setPosition(YogaEdge.TOP, SCENE_Y)
                .setWidth(SCENE_SIZE).setHeight(SCENE_SIZE));
        root.addChild(plate);
    }

    /** Builds the standalone UI for one blueprint file under {@code assets/kuayue/blueprints}. */
    static CarModelUi of(ResourceLocation blueprint) {
        CarModelUi ui = new CarModelUi(blueprint);
        GuiModelPreview preview = BlueprintPreview.open(blueprint);
        if (preview == null) {
            TechTreeSystem.LOGGER.warn("Car model UI opened without a model for {}", blueprint);
        }
        ui.scene.preview(preview);
        return ui;
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

    /** Draws the hosted element tree, then the model into the scene rectangle. */
    void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        var context = com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext.of(null, graphics, mouseX, mouseY,
                partialTick);
        root.drawInBackground(context);
        root.drawContents(context);
        scene.render(graphics, SCENE_X, SCENE_Y, SCENE_SIZE, SCENE_SIZE, partialTick);
    }

    void close() {
        scene.close();
    }

    private UIElement title() {
        Label label = new Label();
        label.setText(Component.translatable("tech_tree.blueprint.title"));
        label.textStyle(t -> t.textColor(INK_STRONG).adaptiveHeight(true));
        label.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                .setPosition(YogaEdge.LEFT, TEXT_X)
                .setPosition(YogaEdge.TOP, 18f)
                .setWidth(TEXT_W));
        return label;
    }

    private UIElement body() {
        Label label = new Label();
        label.setText(Component.translatable("tech_tree.blueprint.description"));
        label.textStyle(t -> t.textColor(INK).adaptiveHeight(true));
        label.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                .setPosition(YogaEdge.LEFT, TEXT_X)
                .setPosition(YogaEdge.TOP, 42f)
                .setWidth(TEXT_W));
        return label;
    }

    /** Wraps the UI in the {@link Renderable} the Kasuga world backend expects. */
    Renderable renderable() {
        return this::render;
    }

    /** Registers the standalone UI on the table model at the given transform. */
    Renderable attach(org.joml.Matrix4f localToWorld) {
        Renderable renderable = renderable();
        if (Constants.UI_BACKEND != null) Constants.UI_BACKEND.addRenderable(renderable, localToWorld);
        return renderable;
    }

    /**
     * Table transform for a board that stands up on the table instead of lying flat on it.
     *
     * <p>{@link TechTreeBoard#tableTransform} lays the UI flat on the table top, which is right for a drawn
     * sheet but wrong for a model: a preview fitted by its own camera would be tipped over. This keeps the
     * model upright, only turns it with the table's facing, and anchors it at the bottom centre of its plate
     * so it rests on the table surface.</p>
     *
     * @param pixelsPerBlock world size of one GUI pixel; the plate is {@link #SCENE_SIZE} pixels wide
     */
    static org.joml.Matrix4f uprightTransform(BlueprintTableBlockEntity table, float pixelsPerBlock) {
        var pos = table.getBlockPos();
        // Bottom centre of the plate in local pixels: the model's feet.
        float anchorX = SCENE_X + SCENE_SIZE * 0.5f;
        float anchorY = SCENE_Y + SCENE_SIZE;
        return new org.joml.Matrix4f()
                .translate(pos.getX() + 0.5f, pos.getY() + TechTreeBoard.TOP_Y, pos.getZ() + 0.5f)
                .rotateY((float) Math.toRadians(table.yaw()))
                // The parchment plate is drawn with GUI coordinates (Y down), so Y is negated. X is negated
                // too: without it the whole plate is mirrored once it is rotated onto the horizontal table
                // top, and every glyph reads backwards from the front of the table.
                .scale(-pixelsPerBlock, -pixelsPerBlock, pixelsPerBlock)
                .translate(-anchorX, -anchorY, 0f);
    }
}
