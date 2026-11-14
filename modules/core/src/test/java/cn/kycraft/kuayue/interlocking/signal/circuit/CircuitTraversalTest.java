package cn.kycraft.kuayue.interlocking.signal.circuit;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 车身扫过绝缘节时的区段累积。
 *
 * <p>覆盖：单区段（旧实现里“只有一个区段时循环体一次都不执行”的退化情形）、长车跨多个区段、
 * 反复跨同一绝缘节、区段尚未传播完成（null）等情况。</p>
 */
class CircuitTraversalTest {

    private static final UUID SEGMENT_A = UUID.randomUUID();
    private static final UUID SEGMENT_B = UUID.randomUUID();
    private static final UUID SEGMENT_C = UUID.randomUUID();

    @Test
    void singleSegmentIsOccupied() {
        CircuitTraversal traversal = new CircuitTraversal();
        traversal.startAt(SEGMENT_A);

        assertEquals(List.of(SEGMENT_A), List.copyOf(traversal.visitedSegments()));
    }

    @Test
    void longTrainAcrossSeveralSegmentsKeepsEverySegment() {
        CircuitTraversal traversal = new CircuitTraversal();
        traversal.startAt(SEGMENT_A);
        traversal.enter(SEGMENT_B);
        traversal.enter(SEGMENT_C);

        assertEquals(List.of(SEGMENT_A, SEGMENT_B, SEGMENT_C), List.copyOf(traversal.visitedSegments()));
    }

    @Test
    void crossingBackAndForthKeepsOneEntryPerSegment() {
        CircuitTraversal traversal = new CircuitTraversal();
        traversal.startAt(SEGMENT_A);
        traversal.enter(SEGMENT_B);
        traversal.enter(SEGMENT_A);
        traversal.enter(SEGMENT_B);

        assertEquals(List.of(SEGMENT_A, SEGMENT_B), List.copyOf(traversal.visitedSegments()));
    }

    @Test
    void repeatedSameBoundaryIsIgnored() {
        CircuitTraversal traversal = new CircuitTraversal();
        traversal.startAt(SEGMENT_A);
        traversal.enter(SEGMENT_A);

        assertEquals(List.of(SEGMENT_A), List.copyOf(traversal.visitedSegments()));
    }

    @Test
    void unresolvedSegmentIsNotOccupied() {
        CircuitTraversal traversal = new CircuitTraversal();
        traversal.startAt(null);
        traversal.enter(null);

        assertTrue(traversal.visitedSegments().isEmpty());
        assertFalse(traversal.visitedSegments().contains(null));
    }
}
