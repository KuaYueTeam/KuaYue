package cn.kycraft.kuayue.parts.tracks;

import cn.kycraft.kuayue.parts.KuaYueTrainPanelModule;
import com.simibubi.create.content.trains.track.TrackBlock;
import com.simibubi.create.content.trains.track.TrackBlockItem;
import io.micronaut.context.annotation.Context;
import lib.kasuga.registration.RegistryGroup;
import lib.kasuga.registration.minecraft.block.BlockReg;
import lib.kasuga.registration.minecraft.block.BlockRegConfigurations;
import net.minecraft.world.level.block.SoundType;

@Context
public class AllTracks {

    public static final RegistryGroup TRACK_REGISTRY = new RegistryGroup()
            .configure(BlockRegConfigurations.adopt(block -> block
                    .noOcclusion()
                    .strength(0.8F, 0.8F)
                    .sound(SoundType.METAL)))
            .setParent(KuaYueTrainPanelModule.REGISTRY_GROUP);

    public static final BlockReg<TrackBlock> STANDARD_TRACK =
            BlockReg.of("standard_track",
                            p -> AllTrackMaterials.STANDARD.createBlock(p))
                    .withBlockItem("standard_track",
                            () -> (block, props) -> new TrackBlockItem(block, props))
                    .setParent(TRACK_REGISTRY);

    public static final BlockReg<TrackBlock> BALLASTLESS_TRACK =
            BlockReg.of("ballastless_track",
                            p -> AllTrackMaterials.BALLASTLESS.createBlock(p))
                    .withBlockItem("ballastless_track",
                            () -> (block, props) -> new TrackBlockItem(block, props))
                    .setParent(TRACK_REGISTRY);

    public static final BlockReg<TrackBlock> TIELESS_TRACK =
            BlockReg.of("tieless_track",
                            p -> AllTrackMaterials.TIELESS.createBlock(p))
                    .withBlockItem("tieless_track",
                            () -> (block, props) -> new TrackBlockItem(block, props))
                    .setParent(TRACK_REGISTRY);

    public static final BlockReg<TrackBlock> METER_TRACK =
            BlockReg.of("meter_track",
                            p -> AllTrackMaterials.METER.createBlock(p))
                    .withBlockItem("meter_track",
                            () -> (block, props) -> new TrackBlockItem(block, props))
                    .setParent(TRACK_REGISTRY);

    public static final BlockReg<TrackBlock> GUARD_TRACK =
            BlockReg.of("guard_track",
                            p -> AllTrackMaterials.GUARD.createBlock(p))
                    .withBlockItem("guard_track",
                            () -> (block, props) -> new TrackBlockItem(block, props))
                    .setParent(TRACK_REGISTRY);
}
