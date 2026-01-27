package de.fhbielefeld.scl.rest.converters;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

/**
 * Convers String date time parameters into LocalDateTime
 *
 * @author Florian Fehring
 */
@Provider
public class LocalDateTimeParamConverterProvider implements ParamConverterProvider {

    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
        if (!rawType.equals(LocalDateTime.class)) {
            return null;
        }

        return (ParamConverter<T>) new ParamConverter<LocalDateTime>() {

            @Override
            public LocalDateTime fromString(String value) {
                if (value == null || value.isEmpty()) {
                    return null;
                }

                try {
                    // 1) Nur Datum → Mitternacht
                    if (value.matches("\\d{4}-\\d{2}-\\d{2}")) {
                        return LocalDate.parse(value).atStartOfDay();
                    }

                    // 2) ISO LocalDateTime
                    return LocalDateTime.parse(value);

                } catch (DateTimeParseException ex) {
                    // Option B: Saubere 400-Fehlermeldung
                    throw new BadRequestException(
                            "Invalid date format for parameter: '" + value +
                            "'. Expected ISO format like 2020-12-24T18:00 or YYYY-MM-DD."
                    );
                }
            }

            @Override
            public String toString(LocalDateTime value) {
                return value.toString();
            }
        };
    }
}