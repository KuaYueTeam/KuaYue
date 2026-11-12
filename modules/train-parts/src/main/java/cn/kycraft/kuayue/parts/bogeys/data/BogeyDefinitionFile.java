package cn.kycraft.kuayue.parts.bogeys.data;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Top-level data-driven bogey definition document.
 *
 * @param sizes  bogey size definitions
 * @param blocks bogey block definitions
 * @param styles bogey style definitions
 */
public record BogeyDefinitionFile(
        @SerializedName("bogey_sizes") List<BogeySizeDefinition> sizes,
        @SerializedName("bogey_blocks") List<BogeyBlockDefinition> blocks,
        @SerializedName("bogey_styles") List<BogeyStyleDefinition> styles
) {
    /**
     * Creates an immutable definition document.
     */
    public BogeyDefinitionFile {
        sizes = sizes == null ? List.of() : List.copyOf(sizes);
        blocks = blocks == null ? List.of() : List.copyOf(blocks);
        styles = styles == null ? List.of() : List.copyOf(styles);
    }
}
