package cn.kycraft.kuayue.content.techtree;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import org.appliedenergistics.yoga.YogaPositionType;

/**
 * Input surface for a blueprint model shown on a table.
 *
 * <p>Carries no drawing of its own: the model is rendered in the world by Kasuga's scene, and a second
 * preview here was composited as a flat quad, which is what showed up as a paper-thin model next to the
 * text. This element only forwards orbit gestures to the host.</p>
 */
final class CarModelScene {

    /** Orbit state, read by the host when it rebuilds the world transform. */
    record Orbit(float yaw, float pitch, float zoom) {
    }

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

    UIElement element() {
        return element;
    }

    Orbit orbit() {
        return new Orbit(yaw, pitch, zoom);
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
        event.stopPropagation();
    }

    private void onWheel(UIEvent event) {
        zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom * (float) Math.exp(event.deltaY * 0.15f)));
        event.stopPropagation();
    }
}
