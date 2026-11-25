package cn.kycraft.kuayue.train;

import cn.kycraft.kuayue.packs.PackLimits;
import cn.kycraft.kuayue.packs.origin.PackOriginIndex;
import cn.kycraft.kuayue.packs.origin.SystemOriginTagIds;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lib.kasuga.core.resource.source.InputPolicy;
import lib.kasuga.core.resource.source.ResourceSnapshot;
import lib.kasuga.core.resource.source.ResourceSource;
import lib.kasuga.core.resource.source.ResourceSourceDescriptor;
import lib.kasuga.core.resource.source.ResourceSources;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

/**
 * Compiles effective train membership into ordinary Minecraft item and block tags.
 */
public final class SystemTrainTags {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final String SOURCE_ID = "kuayue-system-train-tags";
    private static final String ROOT_ID = "trains";

    private SystemTrainTags() {
    }

    public static ResourceSource create(
            KuaYueTrainCatalog catalog,
            PackOriginIndex origin,
            long rank,
            PackLimits limits
    ) throws IOException {
        return create(
                catalog,
                origin,
                rank,
                limits,
                BuiltInRegistries.ITEM::containsKey,
                BuiltInRegistries.BLOCK::containsKey
        );
    }

    static ResourceSource create(
            KuaYueTrainCatalog catalog,
            PackOriginIndex origin,
            long rank,
            PackLimits limits,
            Predicate<ResourceLocation> itemExists,
            Predicate<ResourceLocation> blockExists
    ) throws IOException {
        Map<ResourceLocation, Set<String>> itemTags = new LinkedHashMap<>();
        Map<ResourceLocation, Set<String>> blockTags = new LinkedHashMap<>();
        for (KuaYueTrainDefinition train : catalog.all()) {
            ResourceLocation tag = SystemOriginTagIds.train(train.id());
            Set<String> items = itemTags.computeIfAbsent(tag, ignored -> new TreeSet<>());
            Set<String> blocks = blockTags.computeIfAbsent(tag, ignored -> new TreeSet<>());

            for (ResourceLocation item : train.items()) {
                if (itemExists.test(item)) {
                    items.add(item.toString());
                }
            }
            for (ResourceLocation block : train.blocks()) {
                if (origin.items().containsKey(block)) {
                    items.add(block.toString());
                }
                if (blockExists.test(block)) {
                    blocks.add(block.toString());
                }
            }
            for (ResourceLocation nestedTag : train.itemTags()) {
                items.add("#" + nestedTag);
            }
        }

        InputPolicy policy = new InputPolicy(
                limits.jsonFileBytes(),
                limits.regularFileBytes(),
                limits.packBytes(),
                limits.maxFiles(),
                64,
                1L << 20
        );
        ResourceSourceDescriptor descriptor = new ResourceSourceDescriptor(
                SOURCE_ID,
                SOURCE_ID,
                ROOT_ID,
                Math.toIntExact(rank),
                ResourceSourceDescriptor.Origin.DERIVED
        );
        ResourceSnapshot.Builder builder = new ResourceSnapshot.Builder(descriptor, policy);
        ResourceSnapshot built = null;
        try {
            builder.add(
                    "pack.mcmeta",
                    "pack.mcmeta",
                    () -> new ByteArrayInputStream(bytes(packMetadata()))
            );
            writeTags(builder, "item", itemTags);
            writeTags(builder, "block", blockTags);
            built = builder.build();
            ResourceSource source = ResourceSources.fixed(descriptor, built, policy);
            built = null;
            return source;
        } finally {
            if (built != null) {
                built.close();
            }
            builder.close();
        }
    }

    private static void writeTags(
            ResourceSnapshot.Builder builder,
            String domain,
            Map<ResourceLocation, Set<String>> tags
    ) throws IOException {
        for (Map.Entry<ResourceLocation, Set<String>> entry : tags.entrySet()) {
            String path = "data/" + entry.getKey().getNamespace()
                    + "/tags/" + domain + "/" + entry.getKey().getPath() + ".json";
            byte[] json = bytes(tagJson(entry.getValue()));
            builder.add(path, path, () -> new ByteArrayInputStream(json));
        }
    }

    private static String packMetadata() {
        Map<String, Object> root = Map.of(
                "pack",
                Map.of(
                        "description",
                        "KuaYue generated train membership tags",
                        "pack_format",
                        SharedConstants.getCurrentVersion().getPackVersion(PackType.SERVER_DATA)
                )
        );
        return GSON.toJson(root);
    }

    private static String tagJson(Set<String> members) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("replace", false);
        root.put("values", members);
        return GSON.toJson(root);
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
