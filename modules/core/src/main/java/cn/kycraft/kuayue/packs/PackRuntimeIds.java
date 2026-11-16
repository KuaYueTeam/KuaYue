package cn.kycraft.kuayue.packs;

import net.minecraft.resources.ResourceLocation;

/** Runtime-owned identities shared by bootstrap, datapacks and registrations. */
public final class PackRuntimeIds {
    public static final ResourceLocation BOGEY_BLOCK_ENTITY_ID =
            ResourceLocation.fromNamespaceAndPath("kuayue", "loco_bogey_entity");
    public static final ResourceLocation BOGEY_PROXY_ID =
            ResourceLocation.fromNamespaceAndPath("kuayue", "loco_bogey");

    private PackRuntimeIds() {
    }
}
