package cn.kycraft.kuayue.content.techtree;

import lib.kasuga.rendering.models.mc.Constants;

/**
 * TEMPORARY entry point for the standalone car model scene, wired to sneak-use of the table.
 *
 * <p>Exists only so the scene can be inspected before the real integration; delete this class and its one
 * call site once {@link CarModelUi} is hosted for real.</p>
 */
final class CarModelUiDemo {

    private static boolean visible;

    private CarModelUiDemo() {
    }

    /**
     * Toggles the blueprint model on the table.
     *
     * <p>A toggle rather than an open, because sneak-use is the only binding available for inspection and
     * closing on every use made a second click look like the model had vanished.</p>
     */
    static void toggle(BlueprintTableBlockEntity table) {
        if (visible) {
            close();
            TechTreeSystem.LOGGER.info("Car model demo hidden");
            return;
        }
        open(table);
    }

    /** Client only: puts the car model on the table top. */
    private static void open(BlueprintTableBlockEntity table) {
        if (Constants.UI_BACKEND != null) {
            Constants.UI_BACKEND.openPointer();
        }
        // The model is drawn by Kasuga's scene; no world-space UI host is attached, otherwise its parchment
        // background would hang over the table as a flat sheet.
        BlueprintBlockRenderer.bind(BlueprintPreview.source(BlueprintPreview.DEFAULT), table.getBlockPos());
        BlueprintBlockRenderer.setYaw(table.yaw());
        visible = true;
        TechTreeSystem.LOGGER.info("Car model demo shown for {}", BlueprintPreview.DEFAULT);
    }

    static void close() {
        BlueprintBlockRenderer.close();
        visible = false;
    }

    static boolean isVisible() {
        return visible;
    }
}
