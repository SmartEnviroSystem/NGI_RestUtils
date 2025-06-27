package de.fhbielefeld.scl.rest.util;

import jakarta.json.Json;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;
import java.io.StringReader;

/**
 * This class contains methods for formatting JSON a secure way
 *
 * @author Florian Fehring
 */
public class JSONFormatter {

    public static String escapeForJson(String input) {
        if (input == null) {
            return null;
        }
        StringBuilder escaped = new StringBuilder();
        for (char c : input.toCharArray()) {
            switch (c) {
                case '\"':
                    escaped.append("\\\"");
                    break;
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '\b':
                    escaped.append("\\b");
                    break;
                case '\f':
                    escaped.append("\\f");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                case '/':
                    escaped.append("\\/");
                    break;
                default:
                    if (c < 32 || c > 126) {
                        escaped.append(String.format("\\u%04x", (int) c));
                    } else {
                        escaped.append(c);
                    }
                    break;
            }
        }
        return escaped.toString();
    }
    
    public static JsonValue tryParseOrRaw(String input) {
        try (JsonReader reader = Json.createReader(new StringReader(input))) {
            return reader.readValue();
        } catch (Exception e) {
            // Kein valides JSON – gib als einfacher String zurück
            return Json.createValue(JSONFormatter.escapeForJson(input));
        }
    }
}
