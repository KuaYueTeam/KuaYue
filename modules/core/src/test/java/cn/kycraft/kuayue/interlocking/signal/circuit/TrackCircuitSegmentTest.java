package cn.kycraft.kuayue.interlocking.signal.circuit;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 区段本体：多列车共用同一区段、每 tick 清空（清空是幂等的）。
 */
class TrackCircuitSegmentTest {

    private static final UUID TRAIN_A = UUID.randomUUID();
    private static final UUID TRAIN_B = UUID.randomUUID();

    @Test
    void severalTrainsCanOccupyOneSegment() {
        TrackCircuitSegment segment = new TrackCircuitSegment(UUID.randomUUID());

        segment.occupy(TRAIN_A);
        segment.occupy(TRAIN_B);
        segment.occupy(TRAIN_A);

        assertTrue(segment.hasOccupied());
        assertEquals(2, segment.getOccupyTrainIds().size());
        assertTrue(segment.getOccupyTrainIds().contains(TRAIN_A));
        assertTrue(segment.getOccupyTrainIds().contains(TRAIN_B));
    }

    @Test
    void globalTickClearIsIdempotent() {
        TrackCircuitSegment segment = new TrackCircuitSegment(UUID.randomUUID());
        segment.occupy(TRAIN_A);
        assertTrue(segment.hasOccupied());

        segment.onEarlyGlobalTick();
        segment.onEarlyGlobalTick();

        assertFalse(segment.hasOccupied());
        assertTrue(segment.getOccupyTrainIds().isEmpty());
    }
}
