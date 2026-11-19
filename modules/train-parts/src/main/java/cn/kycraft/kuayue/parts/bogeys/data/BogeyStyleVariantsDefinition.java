package cn.kycraft.kuayue.parts.bogeys.data;

import java.util.List;

/**
 * Variants contributed from another content owner to an existing Create bogey style.
 *
 * @param id       stable identity of this contribution
 * @param style    target {@code bogey_styles} entry
 * @param variants size/block/model mappings appended to that style
 */
public record BogeyStyleVariantsDefinition(
        String id,
        String style,
        List<BogeyStyleDefinition.Variant> variants
) {
    /**
     * Creates and validates a style contribution.
     */
    public BogeyStyleVariantsDefinition {
        id = BogeySchemaStrings.requireId(id, "bogey_style_variants[].id");
        style = BogeySchemaStrings.requireId(style, "bogey_style_variants[].style");
        variants = BogeySchemaStrings.requireValue(
                variants,
                "bogey_style_variants[].variants"
        );
        variants = List.copyOf(variants);
        if (variants.isEmpty()) {
            throw new IllegalArgumentException(
                    "bogey_style_variants[].variants must not be empty"
            );
        }
    }
}
