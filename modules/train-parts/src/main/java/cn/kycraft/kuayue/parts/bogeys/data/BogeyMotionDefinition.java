package cn.kycraft.kuayue.parts.bogeys.data;

/**
 * Data-driven motion profile selected by one or more model parts.
 *
 * @param id      stable motion resource location
 * @param type    motion implementation type
 * @param scale   angular scale or multiplier
 * @param sign    angular direction
 * @param clip    optional formula animation clip
 */
public record BogeyMotionDefinition(
        String id,
        String type,
        float scale,
        float sign,
        String clip
) {
    /**
     * Creates and validates one motion definition.
     */
    public BogeyMotionDefinition {
        id = BogeySchemaStrings.requireId(id, "bogey_motions[].id");
        type = BogeySchemaStrings.requireText(type, "bogey_motions[].type");
        if (!Float.isFinite(scale) || scale <= 0.0f) {
            throw new IllegalArgumentException(
                    "bogey_motions[].scale must be finite and positive"
            );
        }
        if (!Float.isFinite(sign) || sign == 0.0f) {
            throw new IllegalArgumentException(
                    "bogey_motions[].sign must be finite and non-zero"
            );
        }
        switch (type) {
            case "wheel_spin", "rod_rotation" -> clip = null;
            case "formula" -> clip = BogeySchemaStrings.requireId(
                    clip,
                    "bogey_motions[].clip"
            );
            default -> throw new IllegalArgumentException(
                    "Unsupported bogey motion type: " + type
            );
        }
    }
}
