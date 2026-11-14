package cn.kycraft.kuayue.content.techtree;

import lib.kasuga.rendering.models.mc.Constants;
import net.minecraft.client.gui.components.Renderable;

import java.util.HashMap;
import java.util.Map;

/**
 * TEMPORARY entry point for the table model, wired to sneak-use of the table.
 *
 * <p>Exists only so the scene can be inspected before the real integration; delete this class and its call
 * site once {@link CarModelUi} is hosted for real.</p>
 */
final class CarModelUiDemo {

    /** One mounted table: the LDLib2 surface and the renderable registered with Kasuga's UI backend. */
    private record Mounted(CarModelUi ui, Renderable renderable) {
    }

    /** Tables currently showing the model. */
    private static final java.util.Set<net.minecraft.core.BlockPos> MODEL = new java.util.HashSet<>();
    /** Title/description/buttons, mounted per table and independent of the model toggle. */
    private static final Map<net.minecraft.core.BlockPos, Mounted> OVERLAYS = new HashMap<>();

    private CarModelUiDemo() {
    }

    static void toggle(BlueprintTableBlockEntity table) {
        var pos = table.getBlockPos().immutable();
        if (MODEL.contains(pos)) {
            hide(pos);
            TechTreeSystem.LOGGER.info("Car model demo hidden on {}", pos);
            return;
        }
        show(table);
    }

    private static void show(BlueprintTableBlockEntity table) {
        var pos = table.getBlockPos().immutable();
        ensureOverlay(table);
        BlueprintBlockRenderer.bind(pos, BlueprintPreview.DEFAULT, table.yaw());
        MODEL.add(pos);
        TechTreeSystem.LOGGER.info("Car model demo shown on {} for {}", pos, BlueprintPreview.DEFAULT);
    }

    /** Mounts the overlay on the next client tick, from {@code setPlacedBy}. */
    static void scheduleOverlay(net.minecraft.core.BlockPos pos) {
        net.minecraft.client.Minecraft.getInstance().execute(() -> {
            var level = net.minecraft.client.Minecraft.getInstance().level;
            if (level != null && level.getBlockEntity(pos) instanceof BlueprintTableBlockEntity table) {
                ensureOverlay(table);
            }
        });
    }

    /** Mounts the title/description/buttons once per table; independent of the model toggle. */
    static void ensureOverlay(BlueprintTableBlockEntity table) {
        var pos = table.getBlockPos().immutable();
        if (OVERLAYS.containsKey(pos)) return;
        CarModelUi ui = CarModelUi.of();
        Renderable renderable = ui.attach(CarModelUi.tableTopTransform(table, 0.006f));
        OVERLAYS.put(pos, new Mounted(ui, renderable));
    }

    private static void removeOverlay(net.minecraft.core.BlockPos pos) {
        Mounted mounted = OVERLAYS.remove(pos);
        if (mounted == null) return;
        if (Constants.UI_BACKEND != null) Constants.UI_BACKEND.removeRenderable(mounted.renderable());
        mounted.ui().close();
    }

    /** Removes the model of one table; the overlay stays. */
    static void hide(net.minecraft.core.BlockPos pos) {
        MODEL.remove(pos);
        BlueprintBlockRenderer.unbind(pos);
    }

    static void closeAll() {
        for (var pos : java.util.List.copyOf(MODEL)) hide(pos);
        for (var pos : java.util.List.copyOf(OVERLAYS.keySet())) removeOverlay(pos);
        BlueprintBlockRenderer.closeAll();
    }

    /** Drops the overlay of one table, e.g. when the table is broken. */
    static void hideOverlay(net.minecraft.core.BlockPos pos) {
        removeOverlay(pos);
        hide(pos);
    }
}
