package cn.kycraft.kuayue.parts.tracks;

import io.micronaut.context.annotation.Context;
import lib.kasuga.create.registration.track.TrackMaterialReg;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

/** 2.0 Track material declarations. Block ownership is attached by {@link AllTracks}. */
@Context
public final class AllTrackMaterials {

    public static final TrackMaterialReg STANDARD =
            material("standard", "Standard Track")
                    .customModels(
                            model("standard/tie"),
                            model("standard/segment_left"),
                            model("standard/segment_right")
                    )
                    .simpleTrackModelOffset(0.755f)
                    .sleeper(Blocks.ANDESITE_SLAB);
    public static final TrackMaterialReg BALLASTLESS =
            material("ballastless", "Ballastless Track")
                    .customModels(
                            model("ballastless/tie"),
                            model("standard/segment_left"),
                            model("standard/segment_right")
                    )
                    .simpleTrackModelOffset(0.755f)
                    .sleeper(Blocks.ANDESITE_SLAB);
    public static final TrackMaterialReg TIELESS =
            material("tieless", "Tieless Track")
                    .customModels(
                            model("empty_model"),
                            model("standard/segment_left"),
                            model("standard/segment_right")
                    )
                    .simpleTrackModelOffset(0.755f)
                    .sleeper(Blocks.ANDESITE_SLAB);
    public static final TrackMaterialReg METER =
            material("meter", "Meter Track")
                    .customModels(
                            model("meter/tie"),
                            model("standard/segment_left"),
                            model("standard/segment_right")
                    )
                    .simpleTrackModelOffset(0.525f)
                    .sleeper(Blocks.ANDESITE_SLAB);
    public static final TrackMaterialReg GUARD =
            material("guard", "Guard Track")
                    .customModels(
                            model("guard/tie"),
                            model("guard/segment_left"),
                            model("guard/segment_right")
                    )
                    .simpleTrackModelOffset(0.755f)
                    .sleeper(Blocks.ANDESITE_SLAB);

    public AllTrackMaterials() {
    }

    private static TrackMaterialReg material(String path, String name) {
        return new TrackMaterialReg(path)
                .lang(name)
                .particle(ResourceLocation.fromNamespaceAndPath(
                        "kuayue",
                        "block/palettes/stone_types/polished/andesite_cut_polished"
                ));
    }

    private static ResourceLocation model(String path) {
        return ResourceLocation.fromNamespaceAndPath("kuayue", "block/track/" + path);
    }
}
