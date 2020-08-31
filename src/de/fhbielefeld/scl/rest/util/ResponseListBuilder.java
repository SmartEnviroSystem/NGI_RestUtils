package de.fhbielefeld.scl.rest.util;

import de.fhbielefeld.scl.rest.converters.ObjectConverter;
import de.fhbielefeld.scl.rest.exceptions.ObjectConvertException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
//import javax.persistence.Entity;

/**
 * Builder for lists of responses.
 *
 * @author jannik, Florian Fehring
 */
public class ResponseListBuilder {

    private boolean resolveReferences = false;
    private final List<String> attrs = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();
    private final List<Exception> exceptions = new ArrayList<>();

    public ResponseListBuilder() {

    }

    public ResponseListBuilder(boolean isSubList) {
        this.resolveReferences = isSubList;
    }

    /**
     * Adds an object of object-value to this list.
     *
     * @param value Value to add, is automatically converted
     * @return This modified responselist
     */
    public ResponseListBuilder add(Object value) {
        if (value == null) {
            this.attrs.add("null");
        } else if (value instanceof Number) {
            this.attrs.add(value.toString());
        } else if (value instanceof String) {
            String valueStr = (String) value;
            if ((valueStr.startsWith("{") && valueStr.endsWith("}"))
                    || (valueStr.startsWith("[") && valueStr.endsWith("]"))) {
                this.attrs.add(valueStr);
            } else {
                this.attrs.add("\"" + valueStr + "\"");
            }
        } else if (value instanceof Boolean) {
            this.attrs.add((Boolean) value + "");
        } else if (value instanceof ResponseObjectBuilder) {
            ResponseObjectBuilder rob = (ResponseObjectBuilder) value;
            this.attrs.add(rob.toString());
        } else if (value instanceof ResponseListBuilder) {
            ResponseListBuilder rlb = (ResponseListBuilder) value;
            this.attrs.add(rlb.toString());
//        } else if (value.getClass().isAnnotationPresent(Entity.class) && resolveReferences) {
//            try {
//                ResponseObjectBuilder rob = ObjectConverter.objectToResponseObjectBuilder(value);
//                this.attrs.add(rob.toString());
//            } catch (ObjectConvertException ex) {
//                this.errors.add("Error converting list-value: " + ex.getLocalizedMessage());
//                this.exceptions.add(ex);
//
//                ex.printStackTrace();
//            }
//        } else if (value.getClass().isAnnotationPresent(Entity.class)) {
//            // Add reference to subobject
//            String ref = ObjectConverter.objectToRefrence(value);
//            this.add(ref);
        } else if (value instanceof Map) {
            // Case for map values
            Map map = (Map) value;
            ResponseObjectBuilder subrob = new ResponseObjectBuilder();
            for (Object curEntryObj : map.entrySet()) {
                Map.Entry curEntry = (Map.Entry) curEntryObj;
                subrob.add(curEntry.getKey().toString(), curEntry.getValue());
            }
            this.add(subrob);
        } else {           
            // Try convert object to json
            try {
                ResponseObjectBuilder rob = ObjectConverter.objectToResponseObjectBuilder(value);
                this.add(rob);
            } catch (ObjectConvertException ex) {
                this.attrs.add("\"" + value.toString() + "\"");
            }
        }
        return this;
    }

    /**
     * Gets an list of all occured errors
     *
     * @return List of errors
     */
    public List<String> getErrors() {
        return errors;
    }

    /**
     * Gets an list of all occured exceptions
     *
     * @return List of exceptions
     */
    public List<Exception> getExceptions() {
        return exceptions;
    }

    @Override
    public String toString() {
        // Useing fast native implemented toString of list
        return this.attrs.toString();
    }
}
