package in.ac.iiitb.ca.exam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import in.ac.iiitb.ca.exam.GradeCalculator.MarksSample;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class GradeCalculatorTest {

    @Test
    void computesLetterGradesAndBacklogs() {
        assertEquals("A+", GradeCalculator.computeGrade(bd("95"), bd("100")));
        assertEquals("F", GradeCalculator.computeGrade(bd("30"), bd("100")));
        assertTrue(GradeCalculator.isBacklog("F", bd("35"), bd("100")));
        assertTrue(GradeCalculator.isBacklog("D", bd("35"), bd("100")));
        assertFalse(GradeCalculator.isBacklog("D", bd("45"), bd("100")));
    }

    @Test
    void averagesCgpaAndCountsBacklogs() {
        List<MarksSample> samples = List.of(
                new MarksSample(bd("80"), bd("100"), "A"),
                new MarksSample(bd("30"), bd("100"), "F"));

        assertEquals(new BigDecimal("5.50"), GradeCalculator.averageCgpa(samples));
        assertEquals(1, GradeCalculator.countBacklogs(samples));
        assertEquals(new BigDecimal("8.00"), GradeCalculator.gradePoint(bd("80"), bd("100")));
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
