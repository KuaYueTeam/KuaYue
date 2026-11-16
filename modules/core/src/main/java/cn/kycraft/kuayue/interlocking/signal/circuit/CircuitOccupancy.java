package cn.kycraft.kuayue.interlocking.signal.circuit;

import lib.kasuga.create.content.train.signal.CustomTrackSegment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 一台轨道电路设备维护的「本车当前占用哪些区段」集合。
 *
 * <p>三个阶段：</p>
 * <ol>
 *     <li>{@link #reseed(Collection)}：每 tick 用一次完整几何扫描的结果整体替换（列车编组增删、
 *     转向架数量变化、装车/读档、换维度后都靠这个重建，不会累积旧状态）；</li>
 *     <li>{@link #enter(UUID)} / {@link #leave(UUID)}：Create 风格的列车前端/后端监听器在车身
 *     越过绝缘节时即时补一手；</li>
 *     <li>{@link #apply(Function, Consumer)}：把集合落进区段表，逐个遍历（含最后一个），
 *     区段已经不存在的剔除。</li>
 * </ol>
 *
 * <p>旧实现在第 3 步用 {@code for (x = it.next(); it.hasNext(); x = it.next())} 遍历，
 * 条件与步进错位，集合里最后一个区段永远轮不到，只有一个区段时循环体一次都不执行。
 * 这里改为对快照做 for-each，遍历范围由集合本身决定。</p>
 */
public final class CircuitOccupancy {

    private final Set<UUID> segments = new LinkedHashSet<>();

    /** 用一次完整扫描结果整体替换。同一结果重复调用是幂等的。 */
    public void reseed(Collection<UUID> collected) {
        segments.clear();
        if (collected == null) {
            return;
        }
        for (UUID segmentId : collected) {
            if (segmentId != null) {
                segments.add(segmentId);
            }
        }
    }

    /** 列车前端越过绝缘节，进入区段。 */
    public boolean enter(UUID segmentId) {
        return segmentId != null && segments.add(segmentId);
    }

    /** 列车后端离开区段。 */
    public boolean leave(UUID segmentId) {
        return segmentId != null && segments.remove(segmentId);
    }

    /** 只读视图，用于查询/测试。 */
    public Set<UUID> segments() {
        return Collections.unmodifiableSet(segments);
    }

    public boolean isEmpty() {
        return segments.isEmpty();
    }

    public int size() {
        return segments.size();
    }

    /**
     * 把当前占用逐个落进区段表。查不到区段（绝缘节被拆、区段表刚重载）的从集合里剔除。
     *
     * @param lookup     区段查找
     * @param visitor    找到区段后的落库动作
     * @return 成功落库的区段数量
     */
    public int apply(Function<UUID, CustomTrackSegment> lookup, Consumer<TrackCircuitSegment> visitor) {
        int applied = 0;
        for (UUID segmentId : new ArrayList<>(segments)) {
            if (!(lookup.apply(segmentId) instanceof TrackCircuitSegment circuit)) {
                segments.remove(segmentId);
                continue;
            }
            visitor.accept(circuit);
            applied++;
        }
        return applied;
    }
}
