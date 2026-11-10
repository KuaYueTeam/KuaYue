package cn.kycraft.kuayue.parts.tracks;

import cn.kycraft.kuayue.parts.KuaYueTrainPanelModule;
import cn.kycraft.kuayue.parts.bogeys.AllCarriageBogeys;
import cn.kycraft.kuayue.parts.tracks.core.SimpleTrackBlock;
import com.simibubi.create.content.trains.bogey.AbstractBogeyBlock;
import io.micronaut.context.annotation.Context;
import lib.kasuga.create.registration.track.TrackMaterialReg;
import lib.kasuga.create.registration.track.TrackReg;
import lib.kasuga.registration.RegistryGroup;
import lib.kasuga.registration.minecraft.block.BlockRegConfigurations;
import net.minecraft.world.level.block.SoundType;

import java.util.function.Supplier;

@Context
public final class AllTracks {

    public static final RegistryGroup TRACK_REGISTRY = new RegistryGroup()
            .configure(BlockRegConfigurations.adopt(block -> block
                    .noOcclusion()
                    .strength(0.8f, 0.8f)
                    .sound(SoundType.METAL)))
            .setParent(KuaYueTrainPanelModule.REGISTRY_GROUP);

    public static final TrackReg<SimpleTrackBlock> STANDARD_TRACK =
            track("standard_track", AllTrackMaterials.STANDARD,
                    () -> AllCarriageBogeys.PK209P_BLOCK.getEntry());
    public static final TrackReg<SimpleTrackBlock> BALLASTLESS_TRACK =
            track("ballastless_track", AllTrackMaterials.BALLASTLESS,
                    () -> AllCarriageBogeys.PK209P_BLOCK.getEntry());
    public static final TrackReg<SimpleTrackBlock> TIELESS_TRACK =
            track("tieless_track", AllTrackMaterials.TIELESS,
                    () -> AllCarriageBogeys.PK209P_BLOCK.getEntry());
    public static final TrackReg<SimpleTrackBlock> METER_TRACK =
            track("meter_track", AllTrackMaterials.METER,
                    () -> AllCarriageBogeys.MKZ_BLOCK.getEntry());
    public static final TrackReg<SimpleTrackBlock> GUARD_TRACK =
            track("guard_track", AllTrackMaterials.GUARD,
                    () -> AllCarriageBogeys.PK209P_BLOCK.getEntry());

    static {
        bind(AllTrackMaterials.STANDARD, STANDARD_TRACK);
        bind(AllTrackMaterials.BALLASTLESS, BALLASTLESS_TRACK);
        bind(AllTrackMaterials.TIELESS, TIELESS_TRACK);
        bind(AllTrackMaterials.METER, METER_TRACK);
        bind(AllTrackMaterials.GUARD, GUARD_TRACK);
    }

    public AllTracks() {
    }

    private static TrackReg<SimpleTrackBlock> track(
            String name,
            TrackMaterialReg material,
            Supplier<? extends AbstractBogeyBlock<?>> bogey
    ) {
        return new TrackReg<>(
                name,
                material,
                (properties, resolvedMaterial) -> new SimpleTrackBlock(
                        properties,
                        resolvedMaterial,
                        bogey
                )
        ).withBlockProperties(properties -> properties
                        .noOcclusion()
                        .strength(0.8f, 0.8f)
                        .sound(SoundType.METAL))
                .setParent(TRACK_REGISTRY);
    }

    private static void bind(
            TrackMaterialReg material,
            TrackReg<SimpleTrackBlock> track
    ) {
        material.block(track).setParent(TRACK_REGISTRY);
    }
}
