package cn.kycraft.kuayue.content.techtree;

import lib.kasuga.rendering.models.mc.scene.MinecraftModelSource;
import lib.kasuga.rendering.models.mc.scene.display.ModelDisplayTarget;
import lib.kasuga.scene.SceneModels;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/**
 * Shows a blueprint on the blueprint table using Kasuga's world model scene.
 *
 * <p>Earlier attempts drew the structure through {@code snapshot.render} with the caller's {@code
 * MultiBufferSource}. That path only produces geometry when the source is fed Ksg's own
 * {@code PreviewRenderBuffers}, so the blocks never appeared and only the neighbouring text was visible.
 * {@code SceneModels} owns exactly that plumbing: it registers a render pipeline phase, allocates the
 * preview buffers, subtracts the camera position and hands the source a context it can draw with.</p>
 *
 * <p>{@code SceneModels} silently skips every frame whose level differs from the one it was opened for, so
 * the scene is rebuilt whenever the current level changes (leaving and rejoining a world swaps the level
 * instance even though the block entity is unchanged).</p>
 */
final class BlueprintBlockRenderer {

    /** World size of the fitted model's longest axis, in blocks. */
    private static final double FIT_EXTENT = 1.1;

    /** World size of the source structure's longest axis, used to normalise the model into FIT_EXTENT. */
    private static final float SOURCE_SPAN = 16.0f;

    /**
     * Extra lift above the block origin.
     *
     * <p>The drawn table top of the model is above the collision box, so anchoring at y+1.01 leaves the
     * structure sunk into the table; this raises it onto the visible surface.</p>
     */
    private static final float TOP_LIFT = 0.5f;

    private static SceneModels scene;
    private static SceneModels.Entry entry;
    private static ClientLevel sceneLevel;
    private static MinecraftModelSource boundSource;
    private static BlockPos boundTable;
    private static float boundYaw;
    private static boolean reported;

    private BlueprintBlockRenderer() {
    }

    /** Places {@code source} standing on the table; the scene takes ownership of the source. */
    static void bind(@Nullable MinecraftModelSource source, BlockPos table) {
        close();
        boundYaw = 0f;
        boundSource = source;
        boundTable = table.immutable();
        TechTreeSystem.LOGGER.info("Blueprint bind: source {}, table {}",
                source == null ? "null" : source.getClass().getSimpleName(), boundTable);
        openScene();
    }

    /**
     * (Re)creates the scene for the current level.
     *
     * <p>The model source is consumed by each scene, so rebuilding needs a fresh decode; the blueprint
     * bookkeeping in {@link BlueprintPreview} hands out a new source on demand.</p>
     */
    private static void openScene() {
        var minecraft = Minecraft.getInstance();
        sceneLevel = minecraft.level;
        if (boundSource == null || sceneLevel == null || boundTable == null) return;
        scene = SceneModels.open(sceneLevel);
        TechTreeSystem.LOGGER.info("Blueprint scene opened on level {} for table {}",
                System.identityHashCode(sceneLevel), boundTable);
        entry = scene.add(boundSource, new Vec3(boundTable.getX() + 0.5d,
                boundTable.getY() + TechTreeBoard.TOP_Y + TOP_LIFT, boundTable.getZ() + 0.5d));
        applyTransform();
        entry.visible(true);
    }

    private static void applyTransform() {
        if (entry == null) return;
        entry.transform(new Matrix4f()
                .rotateY((float) Math.toRadians(boundYaw))
                .scale((float) (FIT_EXTENT / SOURCE_SPAN))
                .translate(-1.5f, 0.0f, -2.5f));
    }

    /**
     * Keeps the scene valid across world changes.
     *
     * <p>Called once per render pass from the blueprint table: {@code SceneModels} ignores frames whose
     * level it was not opened for, so a stale scene would silently draw nothing forever.</p>
     */
    static void ensureLevel() {
        // Nothing bound means the caller closed the model on purpose; rebuilding here would resurrect it.
        if (boundTable == null || boundSource == null) return;
        var current = Minecraft.getInstance().level;
        if (current == null) return;
        if (scene == null || sceneLevel != current) {
            // The old scene owns its source; ask for a fresh one before rebuilding.
            var source = BlueprintPreview.source(BlueprintPreview.DEFAULT);
            if (source == null) return;
            boundSource = source;
            openScene();
            TechTreeSystem.LOGGER.info("Blueprint scene rebuilt for new level {}",
                    System.identityHashCode(current));
        }
        if (!reported && scene != null) {
            reported = true;
            TechTreeSystem.LOGGER.info("Blueprint scene active on level {}", System.identityHashCode(sceneLevel));
        }
    }

    static void setYaw(float yaw) {
        boundYaw = yaw;
        applyTransform();
    }

    /** Drops the scene, which also closes the model source it owns. */
    static void close() {
        entry = null;
        sceneLevel = null;
        reported = false;
        boundTable = null;
        boundSource = null;
        if (scene != null) {
            scene.close();
            scene = null;
        }
    }

    static boolean isBound() {
        return scene != null;
    }
}
