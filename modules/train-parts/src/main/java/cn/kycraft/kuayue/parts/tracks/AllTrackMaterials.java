package cn.kycraft.kuayue.parts.tracks;

import cn.kycraft.kuayue.parts.KuaYueTrainPanelModule;
import cn.kycraft.kuayue.parts.bogeys.AllCarriageBogeys;
import cn.kycraft.kuayue.parts.tracks.core.SimpleTrackBlock;
import com.simibubi.create.content.trains.track.TrackMaterial;
import com.simibubi.create.content.trains.track.TrackMaterialFactory;
import io.micronaut.context.annotation.Context;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

@Context
public class AllTrackMaterials {

    private static ResourceLocation rl(String path) {
        return ResourceLocation.parse("kuayue:" + path);
    }

    public static final TrackMaterial STANDARD = TrackMaterialFactory.make(rl("standard"))
            .lang("Standard Track")
            .block(() -> () -> AllTracks.STANDARD_TRACK.getEntry())
            .particle(rl("block/palettes/stone_types/polished/andesite_cut_polished"))
            .customBlockFactory((props, material) -> new SimpleTrackBlock(props, material,
                    () -> AllCarriageBogeys.PK209P_BLOCK.getEntry()))
            .standardModels()
            .sleeper(Blocks.ANDESITE_SLAB)
            .build();

    public static final TrackMaterial BALLASTLESS = TrackMaterialFactory.make(rl("ballastless"))
            .lang("Ballastless Track")
            .block(() -> () -> AllTracks.BALLASTLESS_TRACK.getEntry())
            .particle(rl("block/palettes/stone_types/polished/andesite_cut_polished"))
            .customBlockFactory((props, material) -> new SimpleTrackBlock(props, material,
                    () -> AllCarriageBogeys.PK209P_BLOCK.getEntry()))
            .standardModels()
            .sleeper(Blocks.ANDESITE_SLAB)
            .build();

    public static final TrackMaterial TIELESS = TrackMaterialFactory.make(rl("tieless"))
            .lang("Tieless Track")
            .block(() -> () -> AllTracks.TIELESS_TRACK.getEntry())
            .particle(rl("block/palettes/stone_types/polished/andesite_cut_polished"))
            .customBlockFactory((props, material) -> new SimpleTrackBlock(props, material,
                    () -> AllCarriageBogeys.PK209P_BLOCK.getEntry()))
            .standardModels()
            .noRecipeGen()
            .build();

    public static final TrackMaterial METER = TrackMaterialFactory.make(rl("meter"))
            .lang("Meter Track")
            .block(() -> () -> AllTracks.METER_TRACK.getEntry())
            .particle(rl("block/palettes/stone_types/polished/andesite_cut_polished"))
            .customBlockFactory((props, material) -> new SimpleTrackBlock(props, material,
                    () -> AllCarriageBogeys.MKZ_BLOCK.getEntry()))
            .standardModels()
            .sleeper(Blocks.ANDESITE_SLAB)
            .build();

    public static final TrackMaterial GUARD = TrackMaterialFactory.make(rl("guard"))
            .lang("Guard Track")
            .block(() -> () -> AllTracks.GUARD_TRACK.getEntry())
            .particle(rl("block/palettes/stone_types/polished/andesite_cut_polished"))
            .customBlockFactory((props, material) -> new SimpleTrackBlock(props, material,
                    () -> AllCarriageBogeys.PK209P_BLOCK.getEntry()))
            .standardModels()
            .sleeper(Blocks.ANDESITE_SLAB)
            .build();
}
