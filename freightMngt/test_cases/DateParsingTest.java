import main.controller.TrainDispatchController;
import org.junit.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import static org.junit.Assert.*;

/**
 * Unit tests for TrainDispatchController
 * Tests date/time parsing utility method
 */
public class DateParsingTest {

    @Test
    // Test: Parse date time in format yyyy-MM-dd HH:mm:ss
    public void testParseDateTime_StandardFormat() {
        String dateTimeStr = "2025-10-20 09:00:00";
        LocalDateTime result = TrainDispatchController.parseDateTime(dateTimeStr);
        
        assertNotNull("Parsed date time should not be null", result);
        assertEquals(2025, result.getYear());
        assertEquals(10, result.getMonthValue());
        assertEquals(20, result.getDayOfMonth());
        assertEquals(9, result.getHour());
        assertEquals(0, result.getMinute());
        assertEquals(0, result.getSecond());
    }

    @Test
    // Test: Parse date time in format yyyy-MM-dd HH:mm
    public void testParseDateTime_WithoutSeconds() {
        String dateTimeStr = "2025-10-20 14:30";
        LocalDateTime result = TrainDispatchController.parseDateTime(dateTimeStr);
        
        assertNotNull("Parsed date time should not be null", result);
        assertEquals(2025, result.getYear());
        assertEquals(10, result.getMonthValue());
        assertEquals(20, result.getDayOfMonth());
        assertEquals(14, result.getHour());
        assertEquals(30, result.getMinute());
        assertEquals(0, result.getSecond());
    }

    @Test
    // Test: Parse date time in format dd/MM/yyyy HH:mm:ss
    public void testParseDateTime_EuropeanFormat() {
        String dateTimeStr = "20/10/2025 09:00:00";
        LocalDateTime result = TrainDispatchController.parseDateTime(dateTimeStr);
        
        assertNotNull("Parsed date time should not be null", result);
        assertEquals(2025, result.getYear());
        assertEquals(10, result.getMonthValue());
        assertEquals(20, result.getDayOfMonth());
        assertEquals(9, result.getHour());
        assertEquals(0, result.getMinute());
        assertEquals(0, result.getSecond());
    }

    @Test
    // Test: Parse date time in format dd/MM/yyyy HH:mm
    public void testParseDateTime_EuropeanFormatWithoutSeconds() {
        String dateTimeStr = "15/12/2025 23:45";
        LocalDateTime result = TrainDispatchController.parseDateTime(dateTimeStr);
        
        assertNotNull("Parsed date time should not be null", result);
        assertEquals(2025, result.getYear());
        assertEquals(12, result.getMonthValue());
        assertEquals(15, result.getDayOfMonth());
        assertEquals(23, result.getHour());
        assertEquals(45, result.getMinute());
        assertEquals(0, result.getSecond());
    }

    @Test(expected = DateTimeParseException.class)
    // Test: Parse date time throws exception for invalid format
    public void testParseDateTime_InvalidFormat() {
        String invalidDateTimeStr = "invalid date format";
        TrainDispatchController.parseDateTime(invalidDateTimeStr);
    }

    @Test(expected = DateTimeParseException.class)
    // Test: Parse date time with invalid date values
    public void testParseDateTime_InvalidDateValues() {
        String invalidDateStr = "2025-13-45 25:70:99"; // Invalid month, day, hour, minute, second
        TrainDispatchController.parseDateTime(invalidDateStr);
    }

    @Test
    // Test: Parse date time handles different times correctly
    public void testParseDateTime_DifferentTimes() {
        // Midnight
        LocalDateTime midnight = TrainDispatchController.parseDateTime("2025-01-01 00:00:00");
        assertEquals(0, midnight.getHour());
        assertEquals(0, midnight.getMinute());
        
        // Noon
        LocalDateTime noon = TrainDispatchController.parseDateTime("2025-01-01 12:00:00");
        assertEquals(12, noon.getHour());
        assertEquals(0, noon.getMinute());
        
        // End of day
        LocalDateTime endOfDay = TrainDispatchController.parseDateTime("2025-01-01 23:59:59");
        assertEquals(23, endOfDay.getHour());
        assertEquals(59, endOfDay.getMinute());
        assertEquals(59, endOfDay.getSecond());
    }

    @Test
    // Test: Parse date time handles edge dates correctly
    public void testParseDateTime_EdgeDates() {
        // Start of year
        LocalDateTime startOfYear = TrainDispatchController.parseDateTime("2025-01-01 00:00:00");
        assertEquals(1, startOfYear.getMonthValue());
        assertEquals(1, startOfYear.getDayOfMonth());
        
        // End of year
        LocalDateTime endOfYear = TrainDispatchController.parseDateTime("2025-12-31 23:59:59");
        assertEquals(12, endOfYear.getMonthValue());
        assertEquals(31, endOfYear.getDayOfMonth());
    }
}
