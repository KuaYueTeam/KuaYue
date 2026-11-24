package cn.kycraft.kuayue.parts.bogeys.data;

import com.google.gson.annotations.SerializedName;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Data-driven model group used by one or more bogey style variants.
 *
 * @param id      stable model resource location
 * @param yOffset vertical offset from the proxy root
 * @param parts   ordered model parts
 */
public record BogeyModelDefinition(
        String id,
        @SerializedName("y_offset") float yOffset,
        List<Part> parts
) {
    /**
     * Creates and validates one model definition.
     */
    public BogeyModelDefinition {
        id = BogeySchemaStrings.requireId(id, "bogey_models[].id");
        if (!Float.isFinite(yOffset)) {
            throw new IllegalArgumentException("bogey_models[].y_offset must be finite");
        }
        if (parts == null || parts.isEmpty()) {
            throw new IllegalArgumentException("bogey_models[].parts must not be empty");
        }
        parts = List.copyOf(parts);
        Set<String> partIds = new HashSet<>();
        for (Part part : parts) {
            if (!partIds.add(part.id())) {
                throw new IllegalArgumentException(
                        "bogey model " + id + " contains duplicate part id " + part.id()
                );
            }
        }
    }

    /**
     * One model part and its optional motion profile.
     *
     * @param id     stable part ID within the model
     * @param model  model resource
     * @param local  ordered local transform operations
     * @param motion optional motion profile reference
     */
    public record Part(
            String id,
            String model,
            List<LocalOperation> local,
            String motion
    ) {
        /**
         * Creates and validates one model part.
         */
        public Part {
            id = BogeySchemaStrings.requireText(id, "bogey_models[].parts[].id");
            if (!id.matches("[a-z0-9_.-]+")) {
                throw new IllegalArgumentException(
                        "bogey_models[].parts[].id is invalid: " + id
                );
            }
            model = BogeySchemaStrings.requireId(
                    model,
                    "bogey_models[].parts[].model"
            );
            local = local == null ? List.of() : List.copyOf(local);
            motion = motion == null
                    ? null
                    : BogeySchemaStrings.requireId(
                            motion,
                            "bogey_models[].parts[].motion"
                    );
        }
    }

    /**
     * One ordered local transform operation.
     *
     * @param translate optional translation vector
     * @param rotate    optional rotation vector in degrees
     * @param scale     optional scale vector
     */
    public record LocalOperation(
            List<Float> translate,
            List<Float> rotate,
            List<Float> scale
    ) {
        /**
         * Creates and validates one local operation.
         */
        public LocalOperation {
            int present = (translate == null ? 0 : 1)
                    + (rotate == null ? 0 : 1)
                    + (scale == null ? 0 : 1);
            if (present != 1) {
                throw new IllegalArgumentException(
                        "local transform operation must contain exactly one of "
                                + "translate, rotate, or scale"
                );
            }
            if (translate != null) {
                translate = vector(translate, "translate");
            }
            if (rotate != null) {
                rotate = vector(rotate, "rotate");
            }
            if (scale != null) {
                scale = vector(scale, "scale");
            }
        }

        private static List<Float> vector(List<Float> values, String field) {
            if (values.size() != 3) {
                throw new IllegalArgumentException(
                        "local transform " + field + " must contain three numbers"
                );
            }
            for (float value : values) {
                if (!Float.isFinite(value)) {
                    throw new IllegalArgumentException(
                            "local transform " + field + " must be finite"
                    );
                }
            }
            return List.copyOf(values);
        }
    }
}
