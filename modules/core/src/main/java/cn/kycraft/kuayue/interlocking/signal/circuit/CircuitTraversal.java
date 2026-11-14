package cn.kycraft.kuayue.interlocking.signal.circuit;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 轨道电路占用遍历的纯算法部分：把「沿列车车身从车尾扫到车头」时扫到的区段累积成占用集合。
 *
 * <p>语义对齐 Create 的 {@code Train#collectInitiallyOccupiedSignalBlocks()}：占用不是按单个
 * travelling point 算，而是按车身扫过的每一段轨道算；相邻两个采样点之间越过的绝缘节，其内侧区段
 * 同样计入占用。{@link #startAt(UUID)} 对应 Create 里「车尾点所在信号组」那一段，
 * {@link #enter(UUID)} 对应 {@code forEachTravellingPointBackwards} 回调里的 {@code prevGroup} 去重。</p>
 *
 * <p>和 Create 一样按「车身经过的先后顺序」记录，同一个区段反复经过只保留一次；{@code null}
 * 表示该点还没有被绝缘节分裂出区段（传播未完成），不产生占用。</p>
 */
public final class CircuitTraversal {

    private final Set<UUID> segments = new LinkedHashSet<>();
    private UUID cursor;

    /** 从车尾点所在区段开始。允许 {@code null}：此时还没有可占用的区段。 */
    public void startAt(UUID segmentId) {
        this.cursor = segmentId;
        remember(segmentId);
    }

    /**
     * 车身越过一个绝缘节，进入 {@code segmentId} 一侧。
     * 与游标相同（同一区段里连续穿过多个同类边界）时不重复计入。
     */
    public void enter(UUID segmentId) {
        if (segmentId == null || segmentId.equals(cursor)) {
            return;
        }
        this.cursor = segmentId;
        remember(segmentId);
    }

    /** 已经扫到的区段，按第一次出现的顺序。 */
    public Set<UUID> visitedSegments() {
        return Collections.unmodifiableSet(segments);
    }

    private void remember(UUID segmentId) {
        if (segmentId != null) {
            segments.add(segmentId);
        }
    }
}
