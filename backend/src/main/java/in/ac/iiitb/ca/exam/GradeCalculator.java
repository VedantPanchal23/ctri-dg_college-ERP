package in.ac.iiitb.ca.exam;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

/**
 * Simple grading helpers used by exam marks publication and CGPA recalculation.
 */
public final class GradeCalculator {

    private static final BigDecimal PASS_PERCENT = new BigDecimal("40");
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal TEN = new BigDecimal("10");

    private GradeCalculator() {
    }

    public record MarksSample(BigDecimal marksObtained, BigDecimal maxMarks, String grade) {
    }

    public static String computeGrade(BigDecimal marksObtained, BigDecimal maxMarks) {
        BigDecimal percent = percentage(marksObtained, maxMarks);
        if (percent.compareTo(new BigDecimal("90")) >= 0) {
            return "A+";
        }
        if (percent.compareTo(new BigDecimal("80")) >= 0) {
            return "A";
        }
        if (percent.compareTo(new BigDecimal("70")) >= 0) {
            return "B+";
        }
        if (percent.compareTo(new BigDecimal("60")) >= 0) {
            return "B";
        }
        if (percent.compareTo(new BigDecimal("50")) >= 0) {
            return "C";
        }
        if (percent.compareTo(PASS_PERCENT) >= 0) {
            return "D";
        }
        return "F";
    }

    public static boolean isBacklog(String grade, BigDecimal marksObtained, BigDecimal maxMarks) {
        if (grade != null && "F".equalsIgnoreCase(grade.trim())) {
            return true;
        }
        return percentage(marksObtained, maxMarks).compareTo(PASS_PERCENT) < 0;
    }

    public static BigDecimal gradePoint(BigDecimal marksObtained, BigDecimal maxMarks) {
        return percentage(marksObtained, maxMarks)
                .divide(HUNDRED, 6, RoundingMode.HALF_UP)
                .multiply(TEN)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal averageCgpa(List<MarksSample> samples) {
        Objects.requireNonNull(samples, "samples");
        if (samples.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (MarksSample sample : samples) {
            sum = sum.add(gradePoint(sample.marksObtained(), sample.maxMarks()));
        }
        return sum.divide(BigDecimal.valueOf(samples.size()), 2, RoundingMode.HALF_UP);
    }

    public static int countBacklogs(List<MarksSample> samples) {
        Objects.requireNonNull(samples, "samples");
        int count = 0;
        for (MarksSample sample : samples) {
            if (isBacklog(sample.grade(), sample.marksObtained(), sample.maxMarks())) {
                count++;
            }
        }
        return count;
    }

    public static BigDecimal percentage(BigDecimal marksObtained, BigDecimal maxMarks) {
        Objects.requireNonNull(marksObtained, "marksObtained");
        Objects.requireNonNull(maxMarks, "maxMarks");
        if (maxMarks.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("maxMarks must be positive");
        }
        return marksObtained
                .multiply(HUNDRED)
                .divide(maxMarks, 4, RoundingMode.HALF_UP);
    }
}
