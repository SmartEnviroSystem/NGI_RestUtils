package de.fhbielefeld.scl.rest.util;

import de.fhbielefeld.scl.logger.Logger;
import de.fhbielefeld.scl.logger.message.Message;
import de.fhbielefeld.scl.logger.message.MessageLevel;
import de.fhbielefeld.scl.rest.converters.ObjectConverter;
import de.fhbielefeld.scl.rest.exceptions.ObjectConvertException;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.json.JsonValue.ValueType;
import jakarta.json.JsonWriter;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonGeneratorFactory;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Builds responses for REST API answers
 *
 * @author Jannik Malken, Florian Fehring
 */
public class ResponseObjectBuilder extends ApiResponseBuilder {

    private final Map<String, String> attrs = new HashMap<>(); // Replacement for JsonObjectBuilder (experimental and for subjsons only)

    public ResponseObjectBuilder() {
    }

    /**
     * Adds objects data. Adds objects data directly as key, values. Difference
     * to add(String,Object): Not added as subobject.
     *
     * If obj is an Map, all values will be stored with their keys on root level
     * If obj is an Collection, values will be stored within json-array named
     * "list" If obj is some other algorithm will try to convert it to json
     *
     * @param value Object wich information to add
     * @return This modified ResponseObjectBuilder
     */
    public ResponseObjectBuilder add(Object value) {
        JsonObject jobj;

        if (value == null) {
            Message msg = new Message("null values are not allowed as answer content", MessageLevel.ERROR);
            Logger.addMessage(msg);
        } else if (value instanceof JsonObject) {
            jobj = (JsonObject) value;
            // Add values
            for (Entry<String, JsonValue> curEntry : jobj.entrySet()) {
                this.attrs.put(curEntry.getKey(), curEntry.getValue().toString());
            }
        } else if (value instanceof Map) {
            Map map = (Map) value;
            for (Object curEntryObj : map.entrySet()) {
                Entry curEntry = (Entry) curEntryObj;
                if (curEntry.getKey() == null) {
                    this.add("null", curEntry.getValue());
                } else {
                    this.add(curEntry.getKey().toString(), curEntry.getValue());
                }
            }
        } else if (value instanceof Collection) {
            Collection col = (Collection) value;
            this.add("list", col);
        } else if (value instanceof Boolean
                || value instanceof Byte
                || value instanceof Character
                || value instanceof Number) {
            String msgtxt = "ResponseObjectBuiler: There was a >"
                    + value.getClass().getName() + "< value given to rob.add()."
                    + "The add() interface is intended for use with objects"
                    + " not with single values. You should use add(name,value)"
                    + " instead.";
            Message msg = new Message(msgtxt, MessageLevel.WARNING);
            Logger.addDebugMessage(msg);
            this.addWarningMessage(msgtxt);
        } else {
            // Try convert object to json
            try {
                ResponseObjectBuilder rob = ObjectConverter.objectToResponseObjectBuilder(value);
                this.mergeMessages(rob);
                this.attrs.putAll(rob.attrs);
            } catch (ObjectConvertException ex) {
                this.addErrorMessage(ex.getLocalizedMessage());
                this.addException(ex);
            }
        }
        return this;
    }

    /**
     * Adds an key value pair. With automatic type detection.
     *
     * @param key Key of the entry
     * @param value Entries value, given as java object
     * @return This modified ResponseObjectBuilder
     */
    public ResponseObjectBuilder add(String key, Object value) {
        if (value == null) {
            this.attrs.put(key, "null");
        } else if (value instanceof Number) {
            this.attrs.put(key, value.toString());
        } else if (value instanceof Boolean) {
            this.attrs.put(key, (Boolean) value + "");
        } else if (value instanceof String) {
            String valueStr = (String) value;
            if ((valueStr.startsWith("[") && valueStr.endsWith("]"))
                    || (valueStr.startsWith("{") && valueStr.endsWith("}"))) {
                this.attrs.put(key, valueStr);
            } else {
                valueStr = valueStr.replace("\\", "\\\\");
                this.attrs.put(key, "\"" + valueStr + "\"");
            }
        } else if (value instanceof ResponseObjectBuilder) {
            ResponseObjectBuilder rob = (ResponseObjectBuilder) value;
            this.mergeMessages(rob);
            this.attrs.put(key, rob.toString());
        } else if (value instanceof ResponseListBuilder) {
            ResponseListBuilder rlb = (ResponseListBuilder) value;
            this.mergeMessages(rlb);
            this.attrs.put(key, rlb.toString());
        } else if (value instanceof Map) {
            // Case for map values
            Map<?, ?> map = (Map) value;
            ResponseObjectBuilder subrob = new ResponseObjectBuilder();
            map.entrySet().stream().forEach(
                    e -> {
                        Entry entry = (Entry) e;
                        subrob.add(entry.getKey().toString(), entry.getValue());
                    });
            this.add(key, subrob);
            this.mergeMessages(subrob);
        } else if (value instanceof Collection) {
            // Case for collections (list, etc.)
            Collection<?> col = (Collection) value;
            ResponseListBuilder rlb = new ResponseListBuilder();
            col.stream().forEach(
                    e -> rlb.add(e)
            );
            this.mergeMessages(rlb);
            this.add(key, rlb);
        } else if (value instanceof JsonValue) {
            System.out.println("TEST " + key + " JsonValue");
            JsonValue jv = (JsonValue) value;

            if (jv.getValueType() == ValueType.STRING) {
                System.out.println("TEST ValueType.STRING");
                JsonString jstr = (JsonString) jv;
//                this.add(key, jstr.getString());
                this.attrs.put(key, jstr.getString());
            } else {
                System.out.println("TEST ValueType Other");
                StringWriter sw = new StringWriter();
                try (JsonWriter writer = Json.createWriter(sw)) {
                    writer.write(jv);
                }
                this.attrs.put(key, sw.toString());
            }
        } else {
            System.out.println("TEST something other");
            this.addConvertedToString(key, value.getClass());
            this.attrs.put(key, "\"" + value.toString() + "\"");
        }

        return this;
    }

    /**
     * Generates response as json
     *
     * @return Json or JsonP as result.
     */
    @Override
    public String toString() {
        JsonObjectBuilder builder = Json.createObjectBuilder();

        // Basisdaten aus attrs (String → String, aber als JSON-Werte interpretieren)
        for (Map.Entry<String, String> entry : this.attrs.entrySet()) {
            try (JsonReader reader = Json.createReader(new StringReader(entry.getValue()))) {
                JsonValue value = reader.readValue();
                builder.add(entry.getKey(), value);
            } catch (Exception e) {
                builder.add(entry.getKey(), entry.getValue()); // Fallback: roher String
            }
        }

        // Warnings
        if (!warnings.isEmpty()) {
            JsonArrayBuilder warnArr = Json.createArrayBuilder();
            warnings.forEach(warnArr::add);
            builder.add("warnings", warnArr);
        }

        // Errors
        if (!errors.isEmpty()) {
            JsonArrayBuilder errorArr = Json.createArrayBuilder();
            errors.forEach(errorArr::add);
            builder.add("errors", errorArr);
        }

        // Exceptions (escaped)
        if (!exceptions.isEmpty()) {
            JsonArrayBuilder exArr = Json.createArrayBuilder();
            for (Throwable ex : exceptions) {
                String msg = ex.getLocalizedMessage() != null ? ex.getLocalizedMessage() : ex.toString();
                exArr.add(msg);
            }
            builder.add("exceptions", exArr);
        }

        StringWriter sw = new StringWriter();
        try (JsonWriter writer = Json.createWriter(sw)) {
            writer.writeObject(builder.build());
        }

        return sw.toString();
    }

    @Override
    public String toJson() {
        JsonObjectBuilder builder = Json.createObjectBuilder();

        for (Map.Entry<String, String> entry : this.attrs.entrySet()) {
            String key = entry.getKey();
            String rawValue = entry.getValue();

            try (JsonReader reader = Json.createReader(new StringReader(rawValue))) {
                JsonValue parsed = reader.readValue();
                builder.add(key, parsed);
            } catch (Exception e) {
                // Fallback: als sauberer JSON-String speichern
                builder.add(key, rawValue);
            }
        }

        StringWriter sw = new StringWriter();
        try (JsonWriter writer = Json.createWriter(sw)) {
            writer.writeObject(builder.build());
        }

        return sw.toString().substring(1, sw.toString().length() - 1); // Inhalt ohne äußere { }
    }

    @Override
    public boolean toWriter(Writer writer) {
        try {
            JsonGeneratorFactory factory = Json.createGeneratorFactory(null);
            JsonGenerator generator = factory.createGenerator(writer);
            generator.writeStartObject();

            // attrs-Inhalte
            for (Map.Entry<String, String> entry : this.attrs.entrySet()) {
                String key = entry.getKey();
                String rawValue = entry.getValue();
                try (JsonReader reader = Json.createReader(new StringReader(rawValue))) {
                    generator.write(key, reader.readValue()); // korrekt eingebettet
                } catch (Exception e) {
                    generator.write(key, rawValue); // fallback: als String
                }
            }

            if (!warnings.isEmpty()) {
                generator.writeStartArray("warnings");
                for (String warning : warnings) {
                    generator.write(warning);
                }
                generator.writeEnd();
            }

            if (!errors.isEmpty()) {
                generator.writeStartArray("errors");
                for (String error : errors) {
                    generator.write(error);
                }
                generator.writeEnd();
            }

            if (!exceptions.isEmpty()) {
                generator.writeStartArray("exceptions");
                for (Throwable ex : exceptions) {
                    String msg = ex.getLocalizedMessage() != null ? ex.getLocalizedMessage() : ex.toString();
                    generator.write(msg);
                }
                generator.writeEnd();
            }

            generator.writeEnd(); // end of root object
            generator.flush();
            return true;

        } catch (Exception e) {
            return false;
        }
    }
}
