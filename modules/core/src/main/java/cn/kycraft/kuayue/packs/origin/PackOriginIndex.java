package cn.kycraft.kuayue.packs.origin;

import cn.kycraft.kuayue.packs.bootstrap.KuaYuePackBootstrap;
import cn.kycraft.kuayue.packs.bootstrap.StartupPackSnapshot;
import lib.kasuga.registration.data_driven.pipeline.RegistryClaim;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable mapping from successfully registered Item/Block IDs to their declaring pack and parcel.
 */
public record PackOriginIndex(
        Map<ResourceLocation, Product> items,
        Map<ResourceLocation, Product> blocks
) {
    public PackOriginIndex {
        items = Map.copyOf(items);
        blocks = Map.copyOf(blocks);
    }

    public static PackOriginIndex resolve(
            StartupPackSnapshot snapshot,
            List<RegistryClaim> claims,
            Set<String> activePackIds
    ) {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(claims, "claims");
        Objects.requireNonNull(activePackIds, "activePackIds");

        Map<RootKey, Affiliation> affiliations = new LinkedHashMap<>();
        for (StartupPackSnapshot.ManagedPackRoot root : snapshot.resourceRoots()) {
            if (!activePackIds.contains(root.id())) continue;
            root.capture().roots().forEach(captured -> affiliations.put(
                    new RootKey(captured.source().descriptor().sourceId(), captured.rootId()),
                    new Affiliation(root.id(), captured.parcelId() == null
                            ? null
                            : ResourceLocation.parse(captured.parcelId()))
            ));
        }

        Map<ResourceLocation, Product> items = new LinkedHashMap<>();
        Map<ResourceLocation, Product> blocks = new LinkedHashMap<>();
        for (RegistryClaim claim : claims) {
            Affiliation affiliation = affiliations.get(new RootKey(
                    claim.source().sourceId(),
                    claim.source().rootId()
            ));
            if (affiliation == null) continue;
            Product product = new Product(
                    claim.id(),
                    affiliation.packId(),
                    affiliation.parcelId()
            );
            switch (claim.registry()) {
                case "minecraft:item" -> {
                    if (BuiltInRegistries.ITEM.containsKey(claim.id())
                            && BuiltInRegistries.ITEM.get(claim.id()) != Items.AIR) {
                        items.put(claim.id(), product);
                    } else {
                        throw new IllegalStateException(
                                "Claimed item was not registered successfully: " + claim.id()
                        );
                    }
                }
                case "minecraft:block" -> {
                    if (BuiltInRegistries.BLOCK.containsKey(claim.id())) {
                        blocks.put(claim.id(), product);
                    } else {
                        throw new IllegalStateException(
                                "Claimed block was not registered successfully: " + claim.id()
                        );
                    }
                }
                default -> {
                }
            }
        }
        return new PackOriginIndex(items, blocks);
    }

    public static PackOriginIndex current() {
        StartupPackSnapshot snapshot = KuaYuePackBootstrap.snapshot()
                .orElseThrow(() -> new IllegalStateException("Pack snapshot is not initialized"));
        return resolve(
                snapshot,
                cn.kycraft.kuayue.packs.state.PackRuntimeState.registrationClaims(),
                cn.kycraft.kuayue.packs.state.PackRuntimeState.registeredPackIds()
        );
    }

    public List<Product> sortedItems() {
        return items.values().stream()
                .sorted(Comparator.comparing(product -> product.id().toString()))
                .toList();
    }

    public List<Product> sortedBlocks() {
        return blocks.values().stream()
                .sorted(Comparator.comparing(product -> product.id().toString()))
                .toList();
    }

    private record RootKey(String sourceId, String rootId) {
    }

    private record Affiliation(String packId, ResourceLocation parcelId) {
    }

    public record Product(
            ResourceLocation id,
            String packId,
            ResourceLocation parcelId
    ) {
        public Product {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(packId, "packId");
        }
    }
}
