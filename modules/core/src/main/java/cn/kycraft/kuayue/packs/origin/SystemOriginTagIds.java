package cn.kycraft.kuayue.packs.origin;

import cn.kycraft.kuayue.packs.PackPathRules;
import net.minecraft.resources.ResourceLocation;

/** Stable names and reserved paths for generated pack/parcel origin tags. */
public final class SystemOriginTagIds {
    public static final String NAMESPACE = "kuayue";

    private SystemOriginTagIds() {
    }

    public static ResourceLocation pack(String packId) {
        if (!PackPathRules.isPackId(packId)) {
            throw new IllegalArgumentException("Invalid pack ID: " + packId);
        }
        return ResourceLocation.fromNamespaceAndPath(
                NAMESPACE,
                "system/pack/" + packId
        );
    }

    public static ResourceLocation parcel(String packId, ResourceLocation parcelId) {
        pack(packId);
        String suffix = parcelId.getNamespace() + "/" + parcelId.getPath();
        if (!PackPathRules.isSafeRelativePath(suffix)) {
            throw new IllegalArgumentException("Unsafe parcel identity: " + parcelId);
        }
        return ResourceLocation.fromNamespaceAndPath(
                NAMESPACE,
                "system/parcel/" + packId + "/" + suffix
        );
    }

    public static boolean isReservedDataPath(String path) {
        return path.startsWith("data/kuayue/tags/item/system/")
                || path.startsWith("data/kuayue/tags/block/system/")
                || path.startsWith("data/kuayue/tags/items/system/")
                || path.startsWith("data/kuayue/tags/blocks/system/");
    }
}
