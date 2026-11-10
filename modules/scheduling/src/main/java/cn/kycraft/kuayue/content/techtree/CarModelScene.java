package cn.kycraft.kuayue.content.techtree;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import lib.kasuga.rendering.models.mc.backend.GuiModelPreview;
import net.minecraft.client.gui.GuiGraphics;
import org.appliedenergistics.yoga.YogaPositionType;

/**
 * Self-contained scene that shows one blueprint sitting on a desk, as if the thing were really there.
 *
 * <p>Independent of the tech tree on purpose: it owns its preview, its camera and its own input. Hosting it
 * is a matter of adding {@link #element()} to a layout, so the same scene can sit in the blueprint table
 * now and in a separate screen later without touching this class.</p>
 *
 * <p>The default view looks slightly down at the model rather than straight at it, which is what makes it
 * read as an object standing on a table; drag orbits, wheel zooms, double-click returns to that view.</p>
 */
final class CarModelScene {

    /** Shallow look-down: enough elevation to see the roof, not a top-down map. */
    private static final float DEFAULT_YAW = -0.55f;
    private static final float DEFAULT_PITCH = 0.42f;
    private static final float DEFAULT_ZOOM = 1.0f;
    private static final float MIN_ZOOM = 0.4f;
    private static final float MAX_ZOOM = 4.0f;
    private static final float MIN_PITCH = -0.2f;
    private static final float MAX_PITCH = 1.45f;

    private final UIElement element = new UIElement();

    private float yaw = DEFAULT_YAW;
    private float pitch = DEFAULT_PITCH;
    private float zoom = DEFAULT_ZOOM;

    private int dragButton = -1;
    private double dragX;
    private double dragY;
    private boolean dragging;

    private GuiModelPreview preview;

    /**
     * @param width  scene width in GUI pixels
     * @param height scene height in GUI pixels
     */
    CarModelScene(int width, int height) {
        element.setId("car_model_scene");
        element.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                .setWidth(width).setHeight(height));
        element.addEventListener(UIEvents.MOUSE_DOWN, this::onMouseDown);
        element.addEventListener(UIEvents.MOUSE_MOVE, this::onMouseMove);
        element.addEventListener(UIEvents.MOUSE_UP, this::onMouseUp);
        element.addEventListener(UIEvents.DOUBLE_CLICK, this::onDoubleClick);
        element.addEventListener(UIEvents.MOUSE_WHEEL, this::onWheel);
    }

    /** The host adds this element and calls {@link #render} from its own draw pass. */
    UIElement element() {
        return element;
    }

    /** Binds the model; null keeps the scene empty instead of failing the whole board. */
    void preview(GuiModelPreview preview) {
        this.preview = preview;
        if (preview != null && !preview.isClosed()) preview.setView(yaw, pitch, zoom);
    }

    /**
     * Draws the model into the element rectangle.
     *
     * <p>{@link GuiModelPreview#render} composites its framebuffer as a flat quad under the caller's pose, so
     * the pixels lie on whatever plane the pose projects to. The caller therefore supplies a pose whose quad
     * is upright and whose bottom edge touches the table, instead of the flat board transform.</p>
     */
    void render(GuiGraphics graphics, int left, int top, int width, int height, float partialTick) {
        if (preview == null || preview.isClosed() || width <= 0 || height <= 0) return;
        preview.render(graphics, left, top, width, height, partialTick);
    }

    /** Releases the GPU resources of the preview; safe to call twice. */
    void close() {
        if (preview != null && !preview.isClosed()) preview.close();
        preview = null;
    }

    private void onMouseDown(UIEvent event) {
        if (dragButton != -1) return;
        dragButton = event.button;
        dragX = event.x;
        dragY = event.y;
        dragging = false;
        event.stopPropagation();
    }

    private void onMouseMove(UIEvent event) {
        if (dragButton == -1) return;
        double dx = event.x - dragX;
        double dy = event.y - dragY;
        dragX = event.x;
        dragY = event.y;
        if (Math.abs(dx) < 0.5 && Math.abs(dy) < 0.5 && !dragging) return;
        dragging = true;
        yaw += (float) (dx * 0.01f);
        pitch = Math.max(MIN_PITCH, Math.min(MAX_PITCH, pitch + (float) (dy * 0.01f)));
        applyView();
        event.stopPropagation();
    }

    private void onMouseUp(UIEvent event) {
        if (dragButton == -1) return;
        dragButton = -1;
        dragging = false;
        event.stopPropagation();
    }

    private void onDoubleClick(UIEvent event) {
        yaw = DEFAULT_YAW;
        pitch = DEFAULT_PITCH;
        zoom = DEFAULT_ZOOM;
        applyView();
        event.stopPropagation();
    }

    private void onWheel(UIEvent event) {
        zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom * (float) Math.exp(event.deltaY * 0.15f)));
        applyView();
        event.stopPropagation();
    }

    private void applyView() {
        if (preview != null && !preview.isClosed()) preview.setView(yaw, pitch, zoom);
    }
}
