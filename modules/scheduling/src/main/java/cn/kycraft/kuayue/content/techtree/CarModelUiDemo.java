package cn.kycraft.kuayue.content.techtree;

import lib.kasuga.rendering.models.mc.Constants;
import net.minecraft.client.gui.components.Renderable;

import java.util.HashMap;
import java.util.Map;

/**
 * TEMPORARY entry point for the standalone car model scene, wired to sneak-use of the table.
 *
 * <p>Exists only so the scene can be inspected before the real integration; delete this class and its one
 * call site once {@link CarModelUi} is hosted for real.</p>
 */
final class CarModelUiDemo {

    /** One mounted table: the drawn text column and the renderable registered with Kasuga's UI backend. */
    private record Mounted(CarModelUi ui, Renderable renderable) {
    }

    private static final Map<net.minecraft.core.BlockPos, Mounted> MOUNTED = new HashMap<>();

    private CarModelUiDemo() {
    }

    /**
     * Toggles the blueprint model on one table.
     *
     * <p>Per table, not global: hiding one table must not touch the others.</p>
     */
    static void toggle(BlueprintTableBlockEntity table) {
        var pos = table.getBlockPos();
        if (MOUNTED.containsKey(pos)) {
            hide(pos);
            TechTreeSystem.LOGGER.info("Car model demo hidden on {}", pos);
            return;
        }
        show(table);
    }

    private static void show(BlueprintTableBlockEntity table) {
        var pos = table.getBlockPos().immutable();
        // No openPointer() here: that switched Ksg into world-UI pointer mode and nothing switched it back,
        // which left the cursor free and the crosshair unable to turn the camera after the second use.
        // The model is drawn by Kasuga's scene; the UI host only carries the text column beside it.
        CarModelUi ui = CarModelUi.of(BlueprintPreview.DEFAULT);
        Renderable renderable = ui.attach(CarModelUi.uprightTransform(table, 0.005f));
        BlueprintBlockRenderer.bind(pos, BlueprintPreview.DEFAULT, table.yaw());
        MOUNTED.put(pos, new Mounted(ui, renderable));
        TechTreeSystem.LOGGER.info("Car model demo shown on {} for {}", pos, BlueprintPreview.DEFAULT);
    }

    /** Removes the model and the text from one table. */
    static void hide(net.minecraft.core.BlockPos pos) {
        Mounted mounted = MOUNTED.remove(pos);
        if (mounted != null) {
            if (Constants.UI_BACKEND != null) Constants.UI_BACKEND.removeRenderable(mounted.renderable());
            mounted.ui().close();
        }
        BlueprintBlockRenderer.unbind(pos);
    }

    /** Removes everything; used when leaving the world. */
    static void closeAll() {
        for (var pos : java.util.List.copyOf(MOUNTED.keySet())) hide(pos);
        BlueprintBlockRenderer.closeAll();
    }
}
