package cn.kycraft.kuayue.interlocking.graph;

import cn.kycraft.kuayue.interlocking.signal.SignalModule;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.Create;
import com.simibubi.create.content.equipment.bell.SoulParticle;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.*;
import com.simibubi.create.content.trains.station.GlobalStation;
import com.simibubi.create.content.trains.station.StationBlockEntity;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ShriekParticleOption;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class TestItem extends Item {

    private List<InterlockingVirtualTrack> tracks = new ArrayList<>();

    public TestItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide())
            return InteractionResult.SUCCESS;
        if (context.getPlayer() == null)
            return InteractionResult.FAIL;
        BlockPos blockPos = context.getClickedPos();
        BlockState state = context.getLevel().getBlockState(blockPos);
        TrackGraphLocation location = null;
        if (state.is(AllBlocks.TRACK_STATION)) {
            if (!(context.getLevel().getBlockEntity(blockPos) instanceof StationBlockEntity station)) {
                return InteractionResult.FAIL;
            }
            GlobalStation stationEdgePoint = station.getStation();
            if (stationEdgePoint == null)
                return InteractionResult.FAIL;
            location = station.getBehaviour(TrackTargetingBehaviour.TYPE).determineGraphLocation();
        }

        if (location == null)
            return InteractionResult.FAIL;




        try {
            List<InterlockingVirtualTrack> finalVTs = new ArrayList<>();
            List<InterlockingVirtualTrack> vTs = VirtualTrackPropagator.collect(
                    location.graph,
                    location.graph.getConnection(location.edge.map(location.graph::locateNode)),
                    location.position,
                    (e,l) -> e.getEdgeData().next(SignalModule.STATION_ENTRY.getEdgePoint().getEntry(), l)
            );
            int index = 0;
            HashMap<InterlockingVirtualTrack, List<ArmorStand>> armorStandMap = new HashMap<>();
            for (InterlockingVirtualTrack track : vTs) {
                Vec3 xyzA = track.locationA.getLocation().getLocation();
                if(track.edgePointA != null) {
                    xyzA = track.edges.getFirst().getFirst().getPosition(location.graph, track.edgePointA.getLocationOn(track.edges.getFirst().getFirst()) / track.edges.getFirst().getFirst().getLength());
                }
                ArmorStand armorStandA = new ArmorStand(context.getLevel(), xyzA.x, xyzA.y+ 0.5 * index, xyzA.z);
                armorStandA.setCustomName(Component.literal("[A]" + index));

                Vec3 xyzB = track.locationB.getLocation().getLocation();
                if(track.edgePointB != null) {
                    xyzB = track.edges.getLast().getFirst().getPosition(location.graph, track.edgePointB.getLocationOn(track.edges.getLast().getFirst()) / track.edges.getLast().getFirst().getLength());
                }
                ArmorStand armorStandB = new ArmorStand(context.getLevel(), xyzB.x, xyzB.y + 0.5 * index, xyzB.z);
                armorStandB.setCustomName(Component.literal("[B]" + index));
                armorStandA.setCustomNameVisible(true);
                armorStandB.setCustomNameVisible(true);

                armorStandA.setNoGravity(true);
                armorStandB.setNoGravity(true);

                context.getLevel().addFreshEntity(armorStandA);
                context.getLevel().addFreshEntity(armorStandB);

                index ++;

//                for (Couple<TrackEdge> edge : track.edges) {
//                    Vec3 from = edge.getFirst().node1.getLocation().getLocation();
//                    Vec3 to = edge.getFirst().node2.getLocation().getLocation();
//                    Vec3 dir = to.subtract(from).normalize().scale(0.5);
//                    Vec3 pos = from.add(dir);
//                    double length = edge.getFirst().getLength();
//                    double traveled = 0;
//                    while (traveled < length) {
//                        FallingBlockEntity particle = FallingBlockEntity.fall(
//                                context.getLevel(),
//                                new BlockPos(0,0,0),
//                                Blocks.TORCH.defaultBlockState()
//                        );
//                        particle.time = -9999;
//                        particle.setPos(pos.add(new Vec3(0, 2, 0)));
//                        particle.setNoGravity(true);
//                        particle.disableDrop();
//                        context.getLevel().addFreshEntity(particle);
//                        pos = pos.add(dir);
//                        traveled += 0.5;
//                    }
//
//                }

                armorStandMap.put(track, List.of(armorStandA, armorStandB));
            }
            System.out.println("VT size: " + vTs.size());
            for (InterlockingVirtualTrack vT : vTs) {
                try{
                    finalVTs.addAll(vT.split(List.of(EdgePointType.SIGNAL)));
                }catch (Exception e){
//                    armorStandMap.get(vT).forEach(s->s.setCustomName(Component.literal("[PE]").withColor(0xFFFF0000).append(s.getCustomName())));
//                    for (ArmorStand armorStand : armorStandMap.get(vT)) {
//                        armorStand.setCustomName(Component.literal("").append(armorStand.getCustomName()).append(e.getMessage()));
//                    }
                    e.printStackTrace();
                }
            }
            tracks = finalVTs;
            System.out.println("Final VT size: " + finalVTs.size());
            for (InterlockingVirtualTrack track : finalVTs) {
                Vec3 xyzA = track.locationA.getLocation().getLocation();
                if(track.edgePointA != null) {
                    xyzA = track.edges.getFirst().getFirst().getPosition(location.graph, track.edgePointA.getLocationOn(track.edges.getFirst().getFirst()) / track.edges.getFirst().getFirst().getLength());
                }
                ArmorStand armorStandA = new ArmorStand(context.getLevel(), xyzA.x, xyzA.y + 2 * index, xyzA.z);
                armorStandA.setCustomName(Component.literal("[A]" + index));

                Vec3 xyzB = track.locationB.getLocation().getLocation();
                if(track.edgePointB != null) {
                    xyzB = track.edges.getLast().getFirst().getPosition(location.graph, track.edgePointB.getLocationOn(track.edges.getLast().getFirst()) / track.edges.getLast().getFirst().getLength());
                }

                ArmorStand armorStandB = new ArmorStand(context.getLevel(), xyzB.x, xyzB.y + 2 * index, xyzB.z);
                armorStandB.setCustomName(Component.literal("[B]" + index));
                armorStandA.setCustomNameVisible(true);
                armorStandB.setCustomNameVisible(true);

                armorStandA.setNoGravity(true);
                armorStandB.setNoGravity(true);

                context.getLevel().addFreshEntity(armorStandA);
                context.getLevel().addFreshEntity(armorStandB);

                Vec3 from = xyzA;
                Vec3 to = xyzB;
                Vec3 dir = to.subtract(from).normalize().scale(0.5);
                Vec3 pos = from.add(dir);
                double length = xyzA.subtract(xyzB).length();
                double traveled = 0;
                while (traveled < length) {
                    FallingBlockEntity particle = FallingBlockEntity.fall(
                            context.getLevel(),
                            new BlockPos(0,0,0),
                            Blocks.TORCH.defaultBlockState()
                    );
                    particle.time = -9999;
                    particle.setPos(pos.add(new Vec3(0, index * 2, 0)));
                    particle.setNoGravity(true);
                    particle.disableDrop();
                    context.getLevel().addFreshEntity(particle);
                    pos = pos.add(dir);
                    traveled += 0.5;
                }

                index++;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return InteractionResult.SUCCESS;
    }


    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {

    }
}
