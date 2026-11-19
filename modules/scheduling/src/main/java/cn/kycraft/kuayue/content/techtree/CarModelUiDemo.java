package cn.kycraft.kuayue.content.techtree;

import lib.kasuga.rendering.models.mc.Constants;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mounts the blueprint model on a table; driven by sneak-use of the block.
 *
 * <p>The title/description panel that used to be mounted per table is gone: the table surface now carries
 * the tech tree board ({@link TechTreeTableSurface}), which is always on the table, so there is no second
 * overlay to keep in sync with it. What is left is the model toggle the sneak-use entry point drives.</p>
 *
 * <p>Still temporary: this is the entry point for the blueprint scene until it is wired to a real UI
 * action.</p>
 */
final class CarModelUiDemo {

    /** One mounted table: the LDLib2 surface and the renderable registered with Kasuga's UI backend. */
    private record Mounted(CarModelUi ui, Renderable renderable) {
    }

    /** Tables currently showing the model. */
    private static final java.util.Set<BlockPos> MODEL = new java.util.HashSet<>();

    private CarModelUiDemo() {
    }

    static void toggle(BlueprintTableBlockEntity table) {
        BlockPos pos = table.getBlockPos().immutable();
        if (MODEL.contains(pos)) {
            hide(pos);
            TechTreeSystem.logInfo("Blueprint model hidden on {}", pos);
            return;
        }
        BlueprintBlockRenderer.bind(pos, BlueprintPreview.DEFAULT, table.yaw());
        MODEL.add(pos);
        TechTreeSystem.logInfo("Blueprint model shown on {} for {}", pos, BlueprintPreview.DEFAULT);
    }

    /** Removes the model of one table. */
    static void hide(BlockPos pos) {
        MODEL.remove(pos);
        BlueprintBlockRenderer.unbind(pos);
    }

    /**
     * Drops mounts whose table no longer exists.
     *
     * <p>{@code onRemove} is not guaranteed to run before the block entity is gone, so the mounts are
     * validated against the level each tick instead of relying on that callback alone.</p>
     */
    static void pruneStale() {
        var level = net.minecraft.client.Minecraft.getInstance().level;
        if (level == null) return;
        for (BlockPos pos : List.copyOf(MODEL)) {
            if (!(level.getBlockEntity(pos) instanceof BlueprintTableBlockEntity)) hide(pos);
        }
    }

    static void closeAll() {
        for (BlockPos pos : List.copyOf(MODEL)) hide(pos);
        BlueprintBlockRenderer.closeAll();
    }

    /** Drops the model of one table, e.g. when the table is broken. */
    static void hideOverlay(BlockPos pos) {
        hide(pos);
    }
}
