package cn.kycraft.kuayue.interlocking.signal.circuit;

import cn.kycraft.kuayue.interlocking.signal.SignalModule;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.content.trains.signal.TrackEdgePoint;
import lib.kasuga.KasugaLib;
import lib.kasuga.create.content.train.device.TrainDeviceManager;
import lib.kasuga.create.content.train.device.TrainDeviceSystem;
import lib.kasuga.create.content.train.graph.RailwayData;
import lib.kasuga.create.content.train.graph.RailwayManager;
import lib.kasuga.structure.Pair;
import net.createmod.catnip.data.Couple;
import net.minecraft.world.level.Level;

/**
 * 挂在列车上的轨道电路采集设备：每 tick 把「本车占用了哪些区段」写进区段表。
 *
 * <p>占用是派生状态，不存盘：每 tick 先由 {@link TrainSignalCollector} 按车身几何重算一遍
 * （{@link CircuitOccupancy#reseed}），再逐个落进 {@link TrackCircuitSegment}。因此列车装车、
 * 拆解、换维度、读档、车厢增删、转向架数量变化之后，下一 tick 都会自然重建，不会残留旧占用。
 * 前端/后端监听器（{@link #notifySignalFront} / {@link #notifySignalBack}）保持与 Create
 * {@code Train#frontSignalListener()} / {@code backSignalListener()} 相同的语义，在车身越界时
 * 即时维护集合；下一 tick 的几何重算会整体校正，所以监听器漏掉（/tp、装车、拆解）也不会留下脏状态。</p>
 */
public class BogeyCircuitConnectorDevice extends TrainDeviceSystem {

    private final CircuitOccupancy occupancy = new CircuitOccupancy();

    public BogeyCircuitConnectorDevice(TrainDeviceManager manager) {
        super(manager);
    }

    @Override
    public boolean notifySignalFront(Double distance, Pair<TrackEdgePoint, Couple<TrackNode>> pair) {
        if (!(pair.getFirst() instanceof TrackInsulator insulator)) {
            return false;
        }
        // Create: signal.getGroup(node2)，即列车前方进入的一侧
        occupancy.enter(insulator.getGroupId(insulator.isPrimary(pair.getSecond().getSecond())));
        return false;
    }

    @Override
    public boolean notifySignalBack(Double distance, Pair<TrackEdgePoint, Couple<TrackNode>> pair) {
        if (!(pair.getFirst() instanceof TrackInsulator insulator)) {
            return false;
        }
        // Create: signal.getGroup(node1)，即列车后方离开的一侧
        occupancy.leave(insulator.getGroupId(insulator.isPrimary(pair.getSecond().getFirst())));
        return false;
    }

    @Override
    public void notifySignalCollection() {
        // Create 侧重算占用的时机（落地/编组变化），这里也同步刷新一次
        refresh();
    }

    @Override
    public void earlyTick(Level level) {
        refresh();

        RailwayData data = KasugaLib.getBean(RailwayManager.class).getData();
        Train train = this.manager.getTrain();
        occupancy.apply(
                segmentId -> data.getSegment(SignalModule.INSULATOR_SEGMENT_ID, segmentId),
                circuit -> circuit.occupy(train)
        );
    }

    private void refresh() {
        occupancy.reseed(TrainSignalCollector.collectOccupiedSegments(this.manager.getTrain()));
    }
}
