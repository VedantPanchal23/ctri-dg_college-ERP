package in.ac.iiitb.ca.exam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import in.ac.iiitb.ca.exam.SeatAllocationAlgorithm.RoomCapacity;
import in.ac.iiitb.ca.exam.SeatAllocationAlgorithm.SeatAssignment;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SeatAllocationAlgorithmTest {

    @Test
    void allocatesDeterministicallyAcrossRooms() {
        UUID s1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID s2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID s3 = UUID.fromString("00000000-0000-0000-0000-000000000003");

        List<SeatAssignment> result = SeatAllocationAlgorithm.allocate(
                List.of(s1, s2, s3),
                List.of(new RoomCapacity("R1", 2), new RoomCapacity("R2", 2)));

        assertEquals(3, result.size());
        assertEquals(new SeatAssignment(s1, "R1", "1"), result.get(0));
        assertEquals(new SeatAssignment(s2, "R1", "2"), result.get(1));
        assertEquals(new SeatAssignment(s3, "R2", "1"), result.get(2));
    }

    @Test
    void rejectsInsufficientCapacity() {
        UUID s1 = UUID.randomUUID();
        UUID s2 = UUID.randomUUID();
        assertThrows(
                IllegalArgumentException.class,
                () -> SeatAllocationAlgorithm.allocate(
                        List.of(s1, s2), List.of(new RoomCapacity("R1", 1))));
    }
}
