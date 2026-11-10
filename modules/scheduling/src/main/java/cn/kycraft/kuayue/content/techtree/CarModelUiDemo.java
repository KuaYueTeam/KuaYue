package cn.kycraft.kuayue.content.techtree;

import lib.kasuga.rendering.models.mc.Constants;
import net.minecraft.client.gui.components.Renderable;
import org.joml.Matrix4f;

/**
 * TEMPORARY entry point for the standalone car model UI, wired to sneak-use of the table.
 *
 * <p>Exists only so the scene can be inspected before the real integration; delete this class and its one
 * call site once {@link CarModelUi} is hosted for real.</p>
 */
final class CarModelUiDemo {

    /** World size of one GUI pixel; a 150 px plate is therefore three quarters of a block tall. */
    private static final float PIXELS_PER_BLOCK = 0.005f;

    private static CarModelUi ui;
    private static Renderable renderable;

    private CarModelUiDemo() {
    }

    /** Client only: puts the car model on the table top, replacing any previous demo instance. */
    static void open(BlueprintTableBlockEntity table) {
        close();
        if (Constants.UI_BACKEND == null) {
            TechTreeSystem.LOGGER.warn("Car model demo needs the Ksg world UI backend");
            return;
        }
        // No world-space UI host: the model is drawn by Kasuga's scene, so attaching a Renderable here only
        // paints its parchment background as a flat sheet hovering over the table.
        BlueprintBlockRenderer.bind(BlueprintPreview.source(BlueprintPreview.DEFAULT), table.getBlockPos());
        BlueprintBlockRenderer.setYaw(table.yaw());
        TechTreeSystem.LOGGER.info("Car model demo opened for {}", BlueprintPreview.DEFAULT);
        Constants.UI_BACKEND.openPointer();
    }

    static void close() {
        if (renderable != null && Constants.UI_BACKEND != null) {
            Constants.UI_BACKEND.removeRenderable(renderable);
        }
        if (ui != null) ui.close();
        BlueprintBlockRenderer.close();
        renderable = null;
        ui = null;
    }

    /**
     * Upright placement on the table top; the model stands on its plate instead of lying flat like the board.
     */
    private static Matrix4f transformFor(BlueprintTableBlockEntity table) {
        return CarModelUi.uprightTransform(table, PIXELS_PER_BLOCK);
    }
}
