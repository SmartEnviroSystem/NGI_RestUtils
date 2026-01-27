package de.fhbielefeld.scl.rest.converters;

import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.LocalDateTime;

/**
 * Convers String date time parameters into LocalDateTime
 * 
 * @author Florian Fehring
 */
@Provider
public class LocalDateTimeParamConverterProvider implements ParamConverterProvider {

    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
        if (rawType.equals(LocalDateTime.class)) {
            return (ParamConverter<T>) new ParamConverter<LocalDateTime>() {
                @Override
                public LocalDateTime fromString(String value) {
                    return LocalDateTime.parse(value);
                }

                @Override
                public String toString(LocalDateTime value) {
                    return value.toString();
                }
            };
        }
        return null;
    }
}
