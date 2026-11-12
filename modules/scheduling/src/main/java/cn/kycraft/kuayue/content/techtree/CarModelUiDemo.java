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

    /** Tables currently showing the model. */
    private static final java.util.Set<net.minecraft.core.BlockPos> MOUNTED = new java.util.HashSet<>();
    /** Title/description/buttons, mounted per table and independent of the model toggle. */
    private static final Map<net.minecraft.core.BlockPos, Mounted> OVERLAYS = new HashMap<>();

    private CarModelUiDemo() {
    }

    /**
     * Toggles the blueprint model on one table.
     *
     * <p>Per table, not global: hiding one table must not touch the others.</p>
     */
    static void toggle(BlueprintTableBlockEntity table) {
        var pos = table.getBlockPos();
        if (MOUNTED.contains(pos)) {
            hide(pos);
            TechTreeSystem.LOGGER.info("Car model demo hidden on {}", pos);
            return;
        }
        show(table);
    }

    private static void show(BlueprintTableBlockEntity table) {
        var pos = table.getBlockPos().immutable();
        // The overlay is independent of the model: it stays on the table whether or not the model is shown,
        // so it is mounted here and not removed by hide().
        ensureOverlay(table);
        BlueprintBlockRenderer.bind(pos, BlueprintPreview.DEFAULT, table.yaw());
        MOUNTED.add(pos);
        TechTreeSystem.LOGGER.info("Car model demo shown on {} for {}", pos, BlueprintPreview.DEFAULT);
    }

    /** Mounts the title/description/buttons on a table once; they persist independently of the model. */
    static void ensureOverlay(BlueprintTableBlockEntity table) {
        var pos = table.getBlockPos().immutable();
        if (OVERLAYS.containsKey(pos)) return;
        CarModelUi ui = CarModelUi.of();
        Renderable renderable = ui.attach(CarModelUi.tableTopTransform(table, 0.005f));
        OVERLAYS.put(pos, new Mounted(ui, renderable));
    }

    /** Removes the overlay of one table. */
    private static void removeOverlay(net.minecraft.core.BlockPos pos) {
        Mounted mounted = OVERLAYS.remove(pos);
        if (mounted == null) return;
        if (Constants.UI_BACKEND != null) Constants.UI_BACKEND.removeRenderable(mounted.renderable());
        mounted.ui().close();
    }

    /** Removes the model and the text from one table. */
    static void hide(net.minecraft.core.BlockPos pos) {
        // Only the model goes away; the overlay was never part of it.
        MOUNTED.remove(pos);
        BlueprintBlockRenderer.unbind(pos);
    }

    /** Removes everything; used when leaving the world. */
    static void closeAll() {
        for (var pos : java.util.List.copyOf(MOUNTED)) hide(pos);
        for (var pos : java.util.List.copyOf(OVERLAYS.keySet())) removeOverlay(pos);
        BlueprintBlockRenderer.closeAll();
    }

    /** Drops the overlay of one table, e.g. when the table is broken. */
    static void hideOverlay(net.minecraft.core.BlockPos pos) {
        removeOverlay(pos);
    }
}
