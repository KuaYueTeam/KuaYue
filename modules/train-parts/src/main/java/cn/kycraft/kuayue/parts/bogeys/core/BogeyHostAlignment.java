package cn.kycraft.kuayue.parts.bogeys.core;

import lib.kasuga.rendering.models.uml.math.Transform;

/**
 * Host-frame conversions shared by the placed and train bogey proxies.
 */
final class BogeyHostAlignment {
    private BogeyHostAlignment() {
    }

    /**
     * Converts a placed block root into the data-driven model's forward frame.
     *
     * <p>The catalog models use the opposite horizontal axis from Create's placed bogey
     * renderer. The rotation is composed after the block root, so its world position and vertical
     * offset remain unchanged while every model part rotates around the block centre.</p>
     *
     * @param blockRoot root produced from the block position and axis
     * @return an aligned copy
     */
    static Transform placedRoot(Transform blockRoot) {
        return blockRoot.copy().rotate(0f, 180f, 0f, true);
    }
}
