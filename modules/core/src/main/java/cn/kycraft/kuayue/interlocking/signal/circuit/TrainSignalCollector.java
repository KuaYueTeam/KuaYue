package cn.kycraft.kuayue.interlocking.signal.circuit;

import cn.kycraft.kuayue.interlocking.signal.SignalModule;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.entity.TravellingPoint;
import com.simibubi.create.content.trains.graph.EdgeData;
import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.graph.TrackNode;
import lib.kasuga.KasugaLib;
import lib.kasuga.create.content.train.graph.EdgeExtraData;
import lib.kasuga.create.content.train.graph.GraphExtraData;
import lib.kasuga.create.content.train.graph.RailwayManager;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;
import java.util.UUID;

/**
 * 按几何关系推算一列车当前占用哪些轨道电路区段。
 *
 * <p>这是 Create {@code Train#collectInitiallyOccupiedSignalBlocks()} 的 CustomBoundary 版本：
 * 从最后一节车厢的车尾 travelling point 出发，沿整列车车身往车头扫，把车身经过的每个区段都算进来。
 * 与 Create 一致的部分：</p>
 * <ul>
 *     <li>区段判定以「轨道边」为单位（{@link EdgeExtraData#getBoundaryFeature(ResourceLocation)} /
 *     {@link EdgeExtraData#hasCustomBoundaryInThisEdge(ResourceLocation)}），等价于 Create 的
 *     {@code EdgeData#hasSignalBoundaries()} / {@code getEffectiveEdgeGroupId(TrackGraph)}；</li>
 *     <li>车尾点所在区段由最近的绝缘节决定（{@code EdgeData#next(EdgePointType, double)} +
 *     {@code TrackEdgePoint#getLocationOn(TrackEdge)}），方向取 {@code isPrimary(node)}；</li>
 *     <li>车身中间跨过的绝缘节，取「进入一侧」的区段（{@link CircuitTraversal#enter(UUID)}）；</li>
 *     <li>没有绝缘节分割的边（被动区段）不算区段：如实按「没有区段」返回，<b>不</b>拿图 id
 *     之类的替代 UUID 顶上（Create 那边会落到 fallback signal group，KuaYue 的区段表里没有
 *     这种条目，硬替只会把一个不存在的区段混进占用集合）。</li>
 * </ul>
 *
 * <p>与 Create 的差异：本方法不写任何状态、只返回结果，因此每 tick 都可以重算（幂等），
 * 不需要依赖 Create 那套 {@code updateSignalBlocks} 重算触发点；Create 的
 * {@code forEachTravellingPointBackwards} 里「两转向架车厢最后一个点重复指到后转向架」的写法
 * 也原样沿用，保证采样顺序与运行中 Create 的占用判定一致。</p>
 */
public final class TrainSignalCollector {

    private TrainSignalCollector() {
    }

    /**
     * @return 本车当前占用的区段 id；无法定位（换维度/装车中/车身点未连到轨道）时返回空集合
     */
    public static Set<UUID> collectOccupiedSegments(Train train) {
        if (train == null || train.graph == null || train.carriages.isEmpty()) {
            return Set.of();
        }

        TravellingPoint trailingPoint = train.carriages.getLast().getTrailingPoint();
        TrackNode node1 = trailingPoint.node1;
        TrackNode node2 = trailingPoint.node2;
        TrackEdge edge = trailingPoint.edge;
        if (edge == null) {
            return Set.of();
        }

        double position = trailingPoint.position;
        EdgeData signalData = edge.getEdgeData();
        GraphExtraData extraData = KasugaLib.getBean(RailwayManager.class).getData().withGraph(train.graph);
        EdgeExtraData signalDataExtra = extraData.getOrComputeEdgeData(edge);

        CircuitTraversal traversal = new CircuitTraversal();
        ResourceLocation segmentType = SignalModule.INSULATOR_SEGMENT_ID;
        EdgePointType<TrackInsulator> insulatorType = SignalModule.INSULATOR.getEdgePoint().getEntry();

        if (signalDataExtra.hasCustomBoundaryInThisEdge(segmentType)) {
            TrackInsulator nextBoundary = signalData.next(insulatorType, position);
            if (nextBoundary == null) {
                // 车尾点前面（朝 node2）没有绝缘节：它所在区段是最后一个绝缘节的 node2 侧
                TrackInsulator lastBoundary = null;
                TrackInsulator currentBoundary = signalData.next(insulatorType, 0);
                while (currentBoundary != null) {
                    lastBoundary = currentBoundary;
                    currentBoundary = signalData.next(insulatorType, currentBoundary.getLocationOn(edge));
                }
                if (lastBoundary != null) {
                    traversal.startAt(lastBoundary.getGroupId(lastBoundary.isPrimary(node2)));
                }
            } else {
                // 车尾点在下一个绝缘节的 node1 侧
                traversal.startAt(nextBoundary.getGroupId(nextBoundary.isPrimary(node1)));
            }
        } else {
            UUID segmentId = signalDataExtra.getBoundaryFeature(segmentType);
            // 没有绝缘节分割的边不算轨道电路区段：如实按「没有区段」处理，不拿图 id 之类的
            // 替代 UUID 顶上（KuaYue 的区段表里也没有图 id 对应的区段）。
            if (segmentId != null && !segmentId.equals(EdgeExtraData.passiveBoundaryGroup)) {
                traversal.startAt(segmentId);
            }
        }

        TravellingPoint signalScout = new TravellingPoint(node1, node2, edge, position, false);
        train.forEachTravellingPointBackwards((point, distance) ->
                signalScout.travel(train.graph, distance, signalScout.follow(point), (travelDistance, couple) -> {
                    if (!(couple.getFirst() instanceof TrackInsulator insulator)) {
                        return false;
                    }
                    couple.getSecond()
                            .map(side -> insulator.getGroupId(insulator.isPrimary(side)))
                            .forEach(traversal::enter);
                    return false;
                }, signalScout.ignoreTurns()));

        return Set.copyOf(traversal.visitedSegments());
    }
}
