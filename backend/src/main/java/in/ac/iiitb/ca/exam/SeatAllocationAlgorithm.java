package in.ac.iiitb.ca.exam;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Deterministic seat assignment: students are assumed pre-sorted (e.g. by roll number).
 * Rooms are filled in order; seats numbered 1..capacity within each room.
 */
public final class SeatAllocationAlgorithm {

    private SeatAllocationAlgorithm() {
    }

    public record RoomCapacity(String roomCode, int capacity) {
        public RoomCapacity {
            Objects.requireNonNull(roomCode, "roomCode");
            if (roomCode.isBlank()) {
                throw new IllegalArgumentException("roomCode must not be blank");
            }
            if (capacity < 1) {
                throw new IllegalArgumentException("capacity must be at least 1");
            }
        }
    }

    public record SeatAssignment(UUID studentId, String roomCode, String seatNumber) {
    }

    public static List<SeatAssignment> allocate(List<UUID> studentIdsSorted, List<RoomCapacity> rooms) {
        Objects.requireNonNull(studentIdsSorted, "studentIdsSorted");
        Objects.requireNonNull(rooms, "rooms");

        int totalCapacity = rooms.stream().mapToInt(RoomCapacity::capacity).sum();
        if (studentIdsSorted.size() > totalCapacity) {
            throw new IllegalArgumentException(
                    "Insufficient seat capacity: need " + studentIdsSorted.size() + ", have " + totalCapacity);
        }

        List<SeatAssignment> assignments = new ArrayList<>(studentIdsSorted.size());
        int studentIndex = 0;
        for (RoomCapacity room : rooms) {
            for (int seat = 1; seat <= room.capacity() && studentIndex < studentIdsSorted.size(); seat++) {
                UUID studentId = studentIdsSorted.get(studentIndex++);
                assignments.add(new SeatAssignment(studentId, room.roomCode(), String.valueOf(seat)));
            }
        }
        return assignments;
    }
}
