package de.ngi.restutils;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Contains method for parsing date and time
 *
 * @author Florian Fehring
 */
public class DateTimeParser {

    /**
     * Parses a string as timestamp when possible
     *
     * @param input String that is expecced to be a timestamp
     * @return Timestamp if string could be parsed null otherwise
     */
    public static Timestamp parseTimestamp(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }

        input = input.trim();

        // 1. Versuch: ISO mit Offset → z. B. "2025-07-11T14:21:33.788Z"
        try {
            OffsetDateTime odt = OffsetDateTime.parse(input, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            return Timestamp.from(odt.toInstant());
        } catch (Exception ignored) {
        }

        // 2. Versuch: ISO ohne Offset → z. B. "2025-07-11T14:21:33.788"
        try {
            LocalDateTime ldt = LocalDateTime.parse(input, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return Timestamp.valueOf(ldt);
        } catch (Exception ignored) {
        }

        // 3. Versuch: klassische SQL-Formatierung → "2025-07-11 14:21:33.788"
        try {
            return Timestamp.valueOf(input);
        } catch (Exception ignored) {
        }

        // 4. Versuch: Unix-Zeitstempel in Millisekunden → "1720700493788"
        try {
            if (input.matches("\\d{10,}") && Long.parseLong(input) > 1000000000000L) {
                return new Timestamp(Long.parseLong(input));
            }
        } catch (Exception ignored) {
        }

        // Kein valider Zeitstempel
        return null;
    }
}
