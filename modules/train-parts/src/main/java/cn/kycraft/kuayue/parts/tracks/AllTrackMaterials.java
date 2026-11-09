package cn.kycraft.kuayue.parts.tracks;

import io.micronaut.context.annotation.Context;
import lib.kasuga.create.registration.track.TrackMaterialReg;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

/** 2.0 Track material declarations. Block ownership is attached by {@link AllTracks}. */
@Context
public final class AllTrackMaterials {

    public static final TrackMaterialReg STANDARD =
            material("standard", "Standard Track").sleeper(Blocks.ANDESITE_SLAB);
    public static final TrackMaterialReg BALLASTLESS =
            material("ballastless", "Ballastless Track").sleeper(Blocks.ANDESITE_SLAB);
    public static final TrackMaterialReg TIELESS = material("tieless", "Tieless Track");
    public static final TrackMaterialReg METER =
            material("meter", "Meter Track").sleeper(Blocks.ANDESITE_SLAB);
    public static final TrackMaterialReg GUARD =
            material("guard", "Guard Track").sleeper(Blocks.ANDESITE_SLAB);

    public AllTrackMaterials() {
    }

    private static TrackMaterialReg material(String path, String name) {
        return new TrackMaterialReg(path)
                .lang(name)
                .particle(ResourceLocation.fromNamespaceAndPath(
                        "kuayue",
                        "block/palettes/stone_types/polished/andesite_cut_polished"
                ))
                .standardModels();
    }
}
