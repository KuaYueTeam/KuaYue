package cn.kycraft.kuayue.content.techtree;

import lib.kasuga.rendering.models.mc.scene.MinecraftModelSource;
import lib.kasuga.scene.SceneModels;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import lib.kasuga.rendering.models.mc.scene.BlockDimensions;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shows a blueprint on each blueprint table through Kasuga's world model scene.
 *
 * <p>Kasuga's {@code SceneModels} is a world-space display: it registers a render pipeline phase, allocates
 * the preview buffers, subtracts the camera position and hands the source a context it can actually draw
 * with. Drawing a source directly with the caller's buffer source produces no geometry at all.</p>
 *
 * <p>One scene per table, because a scene owns the model source it is given and closes it on
 * {@code close()}. A single shared scene made hiding one table destroy the models on every other table, and
 * made a re-show mount an already-closed source.</p>
 */
final class BlueprintBlockRenderer {

    /** World size of the fitted model's longest axis, in blocks. */
    private static final double FIT_EXTENT = 0.6;

    /**
     * Extra lift above the block origin.
     *
     * <p>The drawn table top of the model is above the collision box, so anchoring at y+1.01 leaves the
     * structure sunk into the table; this raises it onto the visible surface.</p>
     */
    private static final float TOP_LIFT = 0.16f;

    private static final Map<BlockPos, SceneModels> SCENES = new HashMap<>();
    private static final Map<BlockPos, SceneModels.Entry> ENTRIES = new HashMap<>();

    private BlueprintBlockRenderer() {
    }

    /**
     * Mounts the blueprint on one table.
     *
     * <p>Decodes a fresh source every time: the previous one was closed together with its scene, and a
     * closed source draws nothing.</p>
     */
    static void bind(BlockPos table, ResourceLocation blueprint, float yaw) {
        unbind(table);
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        var source = BlueprintPreview.freshSource(blueprint);
        if (source == null) {
            TechTreeSystem.LOGGER.warn("No model to mount on table {}", table);
            return;
        }
        BlockPos key = table.immutable();
        try {
            // Kasuga reports the tight block extents; they drive both the scale and the half-size centring.
            var dimensions = source.dimensions().orElse(null);
            if (dimensions == null) {
                TechTreeSystem.LOGGER.warn("Blueprint {} has no block dimensions for table {}", blueprint, key);
                return;
            }
            SceneModels scene = SceneModels.open(level);
            // The scene entry is placed at the world origin; the full chain lives in one matrix so the four
            // steps run in the required order.
            var entry = scene.add(source, Vec3.ZERO);
            entry.transform(transform(seat(table, yaw), yaw, dimensions));
            entry.visible(true);
            SCENES.put(key, scene);
            ENTRIES.put(key, entry);
            TechTreeSystem.LOGGER.info("Blueprint {} mounted on table {}", blueprint, key);
        } catch (RuntimeException failure) {
            // The scene took ownership of the source, so closing it here would double-free; just report.
            TechTreeSystem.LOGGER.warn("Blueprint {} could not be mounted on table {}", blueprint, key, failure);
        }
    }

    /** Removes the model from one table; the scene closes the source it owns. */
    static void unbind(BlockPos table) {
        SceneModels scene = SCENES.remove(table);
        ENTRIES.remove(table);
        if (scene != null) scene.close();
    }

    /** Removes every mounted model. */
    static void closeAll() {
        for (BlockPos pos : List.copyOf(SCENES.keySet())) unbind(pos);
    }

    static boolean isBound(BlockPos table) {
        return SCENES.containsKey(table);
    }

    /** Where the structure stands: the middle of the table top, lifted onto the visible surface. */
    /** How far the model sits toward the table's own left, in blocks. */
    private static final double SEAT_LEFT = 0.22d;
    /** How far the model sits toward the table's back edge (away from the player). */
    private static final double SEAT_BACK = -0.25d;

    /**
     * Seat the model on the table's left half.
     *
     * <p>Offsets along the table's own left vector rather than world X: a fixed X offset moved the model to
     * the right of the table whenever the table faced east or west.</p>
     */
    /** Seat of the model on one table; also used to anchor the overlay so both move together. */
    static Vec3 seatOf(BlockPos table, float yaw) {
        return seat(table, yaw);
    }

    private static Vec3 seat(BlockPos table, float yaw) {
        double radians = Math.toRadians(yaw);
        // Local left of the model at yaw 0 is -X. After rotateY(yaw) a local (lx, 0, lz) maps to
        // (lx*cos + lz*sin, 0, -lx*sin + lz*cos), so local (-1, 0, 0) becomes (-cos, 0, +sin).
        double dx = Math.cos(radians) * SEAT_LEFT + Math.sin(radians) * SEAT_BACK;
        double dz = -Math.sin(radians) * SEAT_LEFT + Math.cos(radians) * SEAT_BACK;
        return new Vec3(table.getX() + 0.5d + dx,
                table.getY() + TechTreeBoard.TOP_Y + TOP_LIFT, table.getZ() + 0.5d + dz);
    }

    /** Turns the structure with the table and scales it into {@link #FIT_EXTENT} blocks. */
    /** Extra tilt so the model is presented at an angle instead of square-on. */
    /** Oblique presentation angle in the table plane; smaller reads as more square-on. */
    private static final float TILT_YAW = 60f;

    /**
     * Model transform on the table top, in the prescribed order.
     *
     * <p>translate to the anchor, rotate, scale to the carriage size, then shift by half the structure's
     * length and width so the model sits centred on that anchor instead of hanging off its corner.</p>
     */
    private static Matrix4f transform(Vec3 anchor, float yaw, BlockDimensions dimensions) {
        float scale = (float) (FIT_EXTENT / dimensions.longestAxis());
        return new Matrix4f()
                .translate((float) anchor.x, (float) anchor.y, (float) anchor.z)
                .rotateY((float) Math.toRadians(yaw + TILT_YAW))
                .scale(scale)
                .translate(-dimensions.length() / 2f, 0f, -dimensions.width() / 2f);
    }
}
