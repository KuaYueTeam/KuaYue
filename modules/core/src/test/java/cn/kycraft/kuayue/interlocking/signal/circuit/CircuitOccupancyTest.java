package cn.kycraft.kuayue.interlocking.signal.circuit;

import lib.kasuga.create.content.train.signal.CustomTrackSegment;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 设备每 tick 维护的占用集合：整体重算、逐个落库、区段消失后的剔除、列车离开后的清空。
 *
 * <p>旧实现在落库循环里用
 * {@code for (x = it.next(); it.hasNext(); x = it.next())}，集合里最后一个区段永远轮不到，
 * 只有一个区段时循环体一次都不执行；这里的 {@code lastSegmentIsNotSkipped} /
 * {@code singleSegmentIsLanded} 就是那两条回归。</p>
 */
class CircuitOccupancyTest {

    private static final UUID SEGMENT_A = UUID.randomUUID();
    private static final UUID SEGMENT_B = UUID.randomUUID();
    private static final UUID SEGMENT_C = UUID.randomUUID();
    private static final UUID TRAIN_ID = UUID.randomUUID();

    @Test
    void singleSegmentIsLanded() {
        CircuitOccupancy occupancy = new CircuitOccupancy();
        Map<UUID, CustomTrackSegment> table = new HashMap<>();
        TrackCircuitSegment segment = new TrackCircuitSegment(SEGMENT_A);
        table.put(SEGMENT_A, segment);

        occupancy.reseed(Set.of(SEGMENT_A));
        int applied = occupancy.apply(table::get, circuit -> circuit.occupy(TRAIN_ID));

        assertEquals(1, applied);
        assertTrue(segment.hasOccupied(), "单区段时也必须产生占用");
    }

    @Test
    void lastSegmentIsNotSkipped() {
        CircuitOccupancy occupancy = new CircuitOccupancy();
        Map<UUID, CustomTrackSegment> table = new HashMap<>();
        List<TrackCircuitSegment> segments = List.of(
                new TrackCircuitSegment(SEGMENT_A),
                new TrackCircuitSegment(SEGMENT_B),
                new TrackCircuitSegment(SEGMENT_C));
        table.put(SEGMENT_A, segments.get(0));
        table.put(SEGMENT_B, segments.get(1));
        table.put(SEGMENT_C, segments.get(2));

        occupancy.reseed(Set.of(SEGMENT_A, SEGMENT_B, SEGMENT_C));
        int applied = occupancy.apply(table::get, circuit -> circuit.occupy(TRAIN_ID));

        assertEquals(3, applied);
        for (TrackCircuitSegment segment : segments) {
            assertTrue(segment.hasOccupied(), "每个区段都要落库，不能漏掉最后一个");
        }
    }

    @Test
    void missingSegmentIsPruned() {
        CircuitOccupancy occupancy = new CircuitOccupancy();
        Map<UUID, CustomTrackSegment> table = new HashMap<>();
        table.put(SEGMENT_A, new TrackCircuitSegment(SEGMENT_A));

        occupancy.reseed(Set.of(SEGMENT_A, SEGMENT_B));
        int applied = occupancy.apply(table::get, circuit -> circuit.occupy(TRAIN_ID));

        assertEquals(1, applied);
        assertEquals(Set.of(SEGMENT_A), occupancy.segments());
    }

    @Test
    void trainLeavingClearsOccupationWithoutResidue() {
        CircuitOccupancy occupancy = new CircuitOccupancy();
        TrackCircuitSegment segment = new TrackCircuitSegment(SEGMENT_A);
        Map<UUID, CustomTrackSegment> table = Map.of(SEGMENT_A, segment);

        occupancy.reseed(Set.of(SEGMENT_A));
        occupancy.apply(table::get, circuit -> circuit.occupy(TRAIN_ID));
        assertTrue(segment.hasOccupied());

        // 列车拆解/换维度/卸载后几何扫描返回空集
        occupancy.reseed(Set.of());
        occupancy.apply(table::get, circuit -> circuit.occupy(TRAIN_ID));

        assertTrue(occupancy.isEmpty());
        assertEquals(0, occupancy.apply(table::get, circuit -> circuit.occupy(TRAIN_ID)));
        assertTrue(occupancy.segments().isEmpty());
    }

    @Test
    void consistChangeRebuildsInsteadOfAccumulating() {
        CircuitOccupancy occupancy = new CircuitOccupancy();
        Map<UUID, CustomTrackSegment> table = new HashMap<>();

        // 加车厢/加转向架：占用从 1 段变 3 段
        occupancy.reseed(Set.of(SEGMENT_A));
        occupancy.reseed(Set.of(SEGMENT_A, SEGMENT_B, SEGMENT_C));
        assertEquals(Set.of(SEGMENT_A, SEGMENT_B, SEGMENT_C), occupancy.segments());

        // 减车厢/减转向架：旧的区段必须消失
        occupancy.reseed(Set.of(SEGMENT_B));
        assertEquals(Set.of(SEGMENT_B), occupancy.segments());
    }

    @Test
    void reseedIsIdempotent() {
        CircuitOccupancy occupancy = new CircuitOccupancy();
        Set<UUID> collected = new HashSet<>(Set.of(SEGMENT_A, SEGMENT_B));

        occupancy.reseed(collected);
        occupancy.reseed(collected);

        assertEquals(Set.of(SEGMENT_A, SEGMENT_B), occupancy.segments());
    }

    @Test
    void frontEnterAndBackLeaveMirrorCreateListeners() {
        CircuitOccupancy occupancy = new CircuitOccupancy();

        assertTrue(occupancy.enter(SEGMENT_A));
        assertFalse(occupancy.enter(SEGMENT_A), "重复进入同一区段不应重复计数");
        assertTrue(occupancy.enter(SEGMENT_B));
        assertEquals(Set.of(SEGMENT_A, SEGMENT_B), occupancy.segments());

        assertTrue(occupancy.leave(SEGMENT_A));
        assertFalse(occupancy.leave(SEGMENT_A), "重复离开不做二次删除");
        assertFalse(occupancy.enter(null), "未分裂的区段不占用");
        assertEquals(Set.of(SEGMENT_B), occupancy.segments());
    }

    @Test
    void applyIsIdempotentWithinOneTick() {
        CircuitOccupancy occupancy = new CircuitOccupancy();
        TrackCircuitSegment segment = new TrackCircuitSegment(SEGMENT_A);
        Map<UUID, CustomTrackSegment> table = Map.of(SEGMENT_A, segment);
        AtomicInteger landings = new AtomicInteger();

        occupancy.reseed(Set.of(SEGMENT_A));
        occupancy.apply(table::get, circuit -> {
            circuit.occupy(TRAIN_ID);
            landings.incrementAndGet();
        });
        occupancy.apply(table::get, circuit -> {
            circuit.occupy(TRAIN_ID);
            landings.incrementAndGet();
        });

        assertEquals(2, landings.get());
        assertEquals(1, segment.getOccupyTrainIds().size(), "同一区段重复落库只保留一条列车占用");
    }
}
