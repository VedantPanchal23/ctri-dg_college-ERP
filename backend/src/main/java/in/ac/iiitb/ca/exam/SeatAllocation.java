package in.ac.iiitb.ca.exam;

import in.ac.iiitb.ca.common.domain.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "seat_allocations")
public class SeatAllocation extends TenantScopedEntity {

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "exam_schedule_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID examScheduleId;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "student_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID studentId;

    @Column(name = "room_code", nullable = false, length = 64)
    private String roomCode;

    @Column(name = "seat_number", nullable = false, length = 32)
    private String seatNumber;

    public UUID getExamScheduleId() {
        return examScheduleId;
    }

    public void setExamScheduleId(UUID examScheduleId) {
        this.examScheduleId = examScheduleId;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public void setStudentId(UUID studentId) {
        this.studentId = studentId;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }
}
