package cn.kycraft.kuayue.packs.origin;

import cn.kycraft.kuayue.packs.PackLimits;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lib.kasuga.core.resource.source.InputPolicy;
import lib.kasuga.core.resource.source.ResourceSnapshot;
import lib.kasuga.core.resource.source.ResourceSource;
import lib.kasuga.core.resource.source.ResourceSourceDescriptor;
import lib.kasuga.core.resource.source.ResourceSources;
import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Builds a fixed server-data source containing authoritative pack and parcel origin tags. */
public final class SystemOriginTags {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final String SOURCE_ID = "kuayue-system-origin-tags";
    private static final String ROOT_ID = "tags";

    private SystemOriginTags() {
    }

    public static Result create(PackOriginIndex origin, long rank, PackLimits limits)
            throws IOException {
        Map<ResourceLocation, Set<ResourceLocation>> itemTags = new LinkedHashMap<>();
        Map<ResourceLocation, Set<ResourceLocation>> blockTags = new LinkedHashMap<>();
        for (PackOriginIndex.Product product : origin.sortedItems()) {
            add(itemTags, SystemOriginTagIds.pack(product.packId()), product.id());
            if (product.parcelId() != null) {
                add(
                        itemTags,
                        SystemOriginTagIds.parcel(product.packId(), product.parcelId()),
                        product.id()
                );
            }
        }
        for (PackOriginIndex.Product product : origin.sortedBlocks()) {
            add(blockTags, SystemOriginTagIds.pack(product.packId()), product.id());
            if (product.parcelId() != null) {
                add(
                        blockTags,
                        SystemOriginTagIds.parcel(product.packId(), product.parcelId()),
                        product.id()
                );
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
            return new Result(source, origin);
        } finally {
            if (built != null) built.close();
            builder.close();
        }
    }

    private static void writeTags(
            ResourceSnapshot.Builder builder,
            String domain,
            Map<ResourceLocation, Set<ResourceLocation>> tags
    ) throws IOException {
        for (Map.Entry<ResourceLocation, Set<ResourceLocation>> entry : tags.entrySet()) {
            String path = "data/" + entry.getKey().getNamespace()
                    + "/tags/" + domain + "/" + entry.getKey().getPath() + ".json";
            byte[] json = bytes(tagJson(entry.getValue()));
            builder.add(path, path, () -> new ByteArrayInputStream(json));
        }
    }

    private static void add(
            Map<ResourceLocation, Set<ResourceLocation>> tags,
            ResourceLocation tag,
            ResourceLocation member
    ) {
        tags.computeIfAbsent(tag, ignored -> new LinkedHashSet<>()).add(member);
    }

    private static String packMetadata() {
        Map<String, Object> root = Map.of(
                "pack",
                Map.of(
                        "description",
                        "KuaYue generated system origin tags",
                        "pack_format",
                        SharedConstants.getCurrentVersion().getPackVersion(PackType.SERVER_DATA)
                )
        );
        return GSON.toJson(root);
    }

    private static String tagJson(Set<ResourceLocation> members) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("replace", false);
        root.put("values", new TreeSet<>(members.stream()
                .map(ResourceLocation::toString)
                .toList()));
        return GSON.toJson(root);
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    public record Result(ResourceSource source, PackOriginIndex origin) {
    }
}
