package cn.kycraft.kuayue.interlocking.signal.circuit;

import cn.kycraft.kuayue.interlocking.signal.SignalModule;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.content.trains.signal.TrackEdgePoint;
import lib.kasuga.KasugaLib;
import lib.kasuga.create.content.train.device.TrainDeviceManager;
import lib.kasuga.create.content.train.device.TrainDeviceSystem;
import lib.kasuga.create.content.train.graph.RailwayData;
import lib.kasuga.create.content.train.graph.RailwayManager;
import lib.kasuga.create.content.train.signal.CustomTrackSegment;
import lib.kasuga.structure.Pair;
import lib.kasuga.utils.ValueInspectionUtils;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

public class BogeyCircuitConnectorDevice extends TrainDeviceSystem {

    private Set<UUID> occupiedSegments = new HashSet<>();
    private boolean collected = false;

    private boolean enableSignalReceive = false;

    private Set<Object> signalReceived = new HashSet();

    public BogeyCircuitConnectorDevice(TrainDeviceManager manager) {
        super(manager);
    }

    @Override
    public boolean notifySignalFront(Double distance, Pair<TrackEdgePoint, Couple<TrackNode>> pair) {
        if(!(pair.getFirst() instanceof TrackInsulator insulator)) {
            return false;
        }
        UUID groupId = insulator.getGroupId(insulator.isPrimary(pair.getSecond().getSecond()));
        if(groupId != null) {
            this.occupiedSegments.add(groupId);
        }
        return false;
    }

    @Override
    public boolean notifySignalBack(Double distance, Pair<TrackEdgePoint, Couple<TrackNode>> pair) {
        if(!(pair.getFirst() instanceof TrackInsulator insulator)) {
            return false;
        }
        UUID groupId = insulator.getGroupId(insulator.isPrimary(pair.getSecond().getFirst()));
        if(groupId != null){
            this.occupiedSegments.remove(groupId);
        }
        return false;
    }

    @Override
    public void notifySignalCollection() {
        collected = true;
        Set<UUID> collectedSignals = TrainSignalCollector.collectInitiallyOccupiedSignalBlocks(this.manager.getTrain());
        if(collectedSignals == null)
            return;
        this.occupiedSegments.clear();
        this.occupiedSegments.addAll(collectedSignals);
    }

    @Override
    public void earlyTick(Level level) {
        if(!this.collected) {
            this.notifySignalCollection();
        }
        signalReceived.clear();
        RailwayData data = KasugaLib.getBean(RailwayManager.class).getData();
        Iterator<UUID> iterator = occupiedSegments.iterator();
        if(!iterator.hasNext())
            return;
        for (UUID occupiedSegment = iterator.next(); iterator.hasNext(); occupiedSegment = iterator.next()) {
            CustomTrackSegment segment = data.getSegment(SignalModule.INSULATOR_SEGMENT_ID, occupiedSegment);
            if(!(segment instanceof TrackCircuitSegment circuit)) {
                iterator.remove();
                continue;
            }
            circuit.occupy(this.manager.getTrain());
            circuit.receive(signalReceived);
        }
    }

    @Override
    public void tick(Level level) {
        super.tick(level);
        ValueInspectionUtils.INSTANCE.print(this.getClass().getName() + "::occupiedSegments", occupiedSegments, ()->{
            System.out.println("");
            System.out.println("OccupiedSegments changed:");
            for (UUID uuid : occupiedSegments) {
                System.out.println(" - " + uuid.toString());
            }
            System.out.println("");
        });
    }



    @Override
    public void write(CompoundTag systemTag) {
        super.write(systemTag);
        ListTag occupiedList = new ListTag(Tag.TAG_INT_ARRAY);
        for (UUID occupiedSegment : occupiedSegments) {
            IntArrayTag tag = new IntArrayTag(UUIDUtil.uuidToIntArray(occupiedSegment));
            occupiedList.add(tag);
        }
        systemTag.put("OccupiedSegments", occupiedList);
        systemTag.putBoolean("Collected", collected);
    }

    @Override
    public void read(CompoundTag systemTag) {
        super.read(systemTag);
        occupiedSegments.clear();
        ListTag occupiedList = systemTag.getList("OccupiedSegments", Tag.TAG_INT_ARRAY);
        for (int i = 0; i < occupiedList.size(); i++) {
            int[] array = occupiedList.getIntArray(i);
            UUID uuid = UUIDUtil.uuidFromIntArray(array);
            occupiedSegments.add(uuid);
        }
        collected = systemTag.getBoolean("Collected");
    }

    @Override
    public void onSegmentUpdated(ResourceLocation segmentType) {
        if(segmentType == SignalModule.INSULATOR_SEGMENT_ID){
            collected = false;
        }
    }
}
