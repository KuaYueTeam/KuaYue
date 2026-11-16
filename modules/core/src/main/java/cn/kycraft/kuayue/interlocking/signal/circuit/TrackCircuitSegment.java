package cn.kycraft.kuayue.interlocking.signal.circuit;

import com.simibubi.create.content.trains.entity.Train;
import lib.kasuga.create.content.train.signal.CustomTrackSegment;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 轨道电路区段：每 tick 由 {@code RailwayData.onEarlyGlobalTick} 清空，再由各列车的
 * {@link BogeyCircuitConnectorDevice} 按几何结果重新标回，所以占用是每 tick 重算的快照。
 *
 * <p>占用者按列车 id 记录（而不是 {@code Train} 对象）：占用只在本 tick 内有效、不需要跟随
 * 列车生命周期，也不强引用列车对象；语义上与 Create 的 {@code SignalEdgeGroup#trains} 一致
 * （每 tick 清空、同一 tick 内可有多列车）。</p>
 */
public class TrackCircuitSegment extends CustomTrackSegment {

    protected final Set<UUID> occupyTrainIds = new LinkedHashSet<>();

    public TrackCircuitSegment(UUID segmentId) {
        super(segmentId);
    }

    @Override
    public void onEarlyGlobalTick() {
        occupyTrainIds.clear();
    }

    public void occupy(Train train) {
        if (train != null) {
            occupy(train.id);
        }
    }

    /** 按列车 id 占用（设备落库与测试共用）。 */
    void occupy(UUID trainId) {
        if (trainId != null) {
            occupyTrainIds.add(trainId);
        }
    }

    public boolean hasOccupied() {
        return !occupyTrainIds.isEmpty();
    }

    public void receive(Set<Object> signalReceived) {
    }

    public Set<UUID> getOccupyTrainIds() {
        return Collections.unmodifiableSet(occupyTrainIds);
    }
}
