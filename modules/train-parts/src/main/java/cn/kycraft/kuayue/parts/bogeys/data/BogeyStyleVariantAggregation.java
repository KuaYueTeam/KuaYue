package cn.kycraft.kuayue.parts.bogeys.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ordered variant ledger shared by a base style and every contribution applied to it.
 */
final class BogeyStyleVariantAggregation {
    private final String styleId;
    private final Map<String, String> sizeSources = new LinkedHashMap<>();
    private final Map<String, String> blockSources = new LinkedHashMap<>();
    private final List<BogeyStyleDefinition.Variant> variants = new ArrayList<>();

    BogeyStyleVariantAggregation(String styleId) {
        this.styleId = styleId;
    }

    void add(BogeyStyleDefinition.Variant variant, String source) {
        String sizeId = variant.size();
        String blockId = variant.block();
        String priorSizeSource = sizeSources.putIfAbsent(sizeId, source);
        if (priorSizeSource != null) {
            throw new IllegalArgumentException(
                    "Bogey style " + styleId + " configures size twice: " + sizeId
                            + " from " + priorSizeSource + " and " + source
            );
        }
        String priorBlockSource = blockSources.putIfAbsent(blockId, source);
        if (priorBlockSource != null) {
            throw new IllegalArgumentException(
                    "Bogey style " + styleId + " configures block twice: " + blockId
                            + " from " + priorBlockSource + " and " + source
            );
        }
        variants.add(variant);
    }

    List<BogeyStyleDefinition.Variant> variants() {
        return List.copyOf(variants);
    }

    int size() {
        return variants.size();
    }
}
