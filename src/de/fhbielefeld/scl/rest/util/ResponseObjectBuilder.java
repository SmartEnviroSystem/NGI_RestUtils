package de.fhbielefeld.scl.rest.util;

import de.fhbielefeld.scl.logger.Logger;
import de.fhbielefeld.scl.logger.message.Message;
import de.fhbielefeld.scl.logger.message.MessageLevel;
import de.fhbielefeld.scl.rest.converters.ObjectConverter;
import de.fhbielefeld.scl.rest.exceptions.ObjectConvertException;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;
//import javax.persistence.Entity;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

/**
 * Builds responses for REST API answers
 *
 * @author Jannik Malken, Florian Fehring
 */
public class ResponseObjectBuilder {

    private static boolean debugmode = false;
    private boolean resolveReferences = true;

    private Response.Status status = null;
    private NewCookie cookie = null;
    private String downloadFileName = null;
    private final Map<String, String> attrs = new HashMap<>(); // Replacement for JsonObjectBuilder (experimental and for subjsons only)
    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private final List<Throwable> exceptions = new ArrayList<>();

    public ResponseObjectBuilder() {

    }

    public ResponseObjectBuilder(boolean resolveReferences) {
        this.resolveReferences = resolveReferences;
    }

    /**
     * Sets the state of the debugmode
     *
     * @param _debugmode true for enabling
     */
    public static void setDebugMode(boolean _debugmode) {
        debugmode = _debugmode;
    }

    /**
     * Delivers the state of the debugmode
     *
     * @return true if debugmode is enabled
     */
    public static boolean getDebugMode() {
        return debugmode;
    }

    /**
     * Sets the status for this response.
     *
     * @param status Status, one of standard Response.Status
     * @return
     */
    public ResponseObjectBuilder setStatus(Response.Status status) {
        if (this.status == null || this.status == Response.Status.OK) {
            this.status = status;
        }
        return this;
    }

    public Response.Status getStatus() {
        return this.status;
    }

    /**
     * Sets an cookie that should be delivered with the response
     *
     * @param name Name of the cookie
     * @param value Cookies value
     * @param maxAge Maximum age of cookie
     * @return
     */
    public ResponseObjectBuilder setCookie(String name, String value, int maxAge) {
        // Defines the cookie without domain, path and comment. Send over http and https and without httpOnly mode (allow access with javascript)
        this.cookie = new NewCookie(name, value, "/SmartMonitoringBackend", null, null, maxAge, false, false);
        return this;
    }

    /**
     * Declares the generated response as downloadable. The given filename will
     * be the suggested filename for download in client.
     *
     * @param filename
     */
    public void setDownloadFileName(String filename) {
        this.downloadFileName = filename;
    }

    /**
     * Adds an errormssage to this response.
     *
     * @param errorMessage Error message
     * @return This modified responsebuilder
     */
    public ResponseObjectBuilder addErrorMessage(String errorMessage) {
        this.errors.add(errorMessage);
        return this;
    }

    /**
     * Adds an warning message.
     *
     * @param warningMessage Message to warn
     * @return This modified responsebuilder
     */
    public ResponseObjectBuilder addWarningMessage(String warningMessage) {
        this.warnings.add(warningMessage);
        return this;
    }

    /**
     * Adds an exception that should be delivered in the response (within debug
     * mode only)
     *
     * @param ex Exception to deliver
     * @return The modified responsebuilder
     */
    public ResponseObjectBuilder addException(Throwable ex) {
        this.exceptions.add(ex);
        return this;
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
            this.attrs.put(key, rob.toString());
        } else if (value instanceof ResponseListBuilder) {
            ResponseListBuilder rlb = (ResponseListBuilder) value;
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
        } else if (value instanceof Collection) {
            // Case for collections (list, etc.)
            Collection<?> col = (Collection) value;
            ResponseListBuilder rlb = new ResponseListBuilder(this.resolveReferences);
            col.stream().forEach(
                    e -> rlb.add(e)
            );
            this.transferErrorsFromListBuilder(key, rlb);
            this.add(key, rlb);
//        } else if (value.getClass().isAnnotationPresent(Entity.class)) {
//            // Add reference to subobject
//            String ref = ObjectConverter.objectToRefrence(value);
//            this.add(key, ref);
        } else if (value instanceof JsonValue) {
            JsonValue jv = (JsonValue) value;
            if (jv.getValueType() == ValueType.STRING) {
                JsonString jstr = (JsonString) jv;
                this.add(key, jstr.getString());
            } else {
                this.attrs.put(key, jv.toString());
            }
        } else {
            String wrnmsg = "Value >" + key + "< of type >"
                    + value.getClass().getSimpleName()
                    + "< converted to string representation.";
            Message msg = new Message(wrnmsg, MessageLevel.WARNING);
            Logger.addDebugMessage(msg);
            this.addWarningMessage(wrnmsg);
            this.attrs.put(key, "\"" + value.toString() + "\"");
        }

        return this;
    }

    /**
     * Fills the data from the given list with maps into this response object.
     *
     * @param name Name of the array element in json, where to find the maps
     * @param maplist List of maps with key-value pairs.
     * @return This modified ResponseObjectBuilder
     * @deprecated To be replaced by add(key,value) implementation
     */
    @Deprecated
    public ResponseObjectBuilder add(String name, List<Map<String, Object>> maplist) {
        ResponseListBuilder rlb = new ResponseListBuilder(true);
        for (Map<String, Object> curMap : maplist) {
            ResponseObjectBuilder curRob = new ResponseObjectBuilder();
            for (Entry<String, Object> curEntry : curMap.entrySet()) {
                curRob.add(curEntry.getKey(), curEntry.getValue());
            }
            rlb.add(curRob);
        }
        this.transferErrorsFromListBuilder(name, rlb);
        this.add(name, rlb);

        return this;
    }

    /**
     * Creates an response object as stated in jaxws.rs standard
     *
     * @return
     */
    public Response toResponse() {
        if (this.status == null) {
            this.status = Response.Status.INTERNAL_SERVER_ERROR;
            this.addErrorMessage("There was no status set for this response");
        }
        ResponseBuilder rb = Response.status(this.status);
        if (this.cookie != null) {
            rb.cookie(this.cookie);
        }
        if (this.downloadFileName != null) {
            rb.header("Content-Disposition", "attachment; filename=" + this.downloadFileName);
        }

        rb.entity(this.toString());
        return rb.build();
    }

    /**
     * Creates a response object with streaming capability. EXPERIMENTAL! Does
     * not support status flags, cookies or fileDownloadHeader
     *
     * @return
     */
    public Response toResponseStream() {
        if (this.status == null) {
            this.status = Response.Status.INTERNAL_SERVER_ERROR;
            this.addErrorMessage("There was no status set for this response");
        }
        ResponseBuilder rb = Response.status(this.status);
        if (this.cookie != null) {
            rb.cookie(this.cookie);
        }
        if (this.downloadFileName != null) {
            rb.header("Content-Disposition", "attachment; filename=" + this.downloadFileName);
        }

        StreamingOutput stream = new StreamingOutput() {
            @Override
            public void write(OutputStream out) throws IOException, WebApplicationException {
                boolean prevCont = false;
                Writer writer = new BufferedWriter(new OutputStreamWriter(out));
                writer.write("{");
                if (!attrs.isEmpty()) {
                    int size = attrs.size();
                    int current = 0;
                    for (Entry<String, String> curAttr : attrs.entrySet()) {
                        current++;
                        writer.write("\"" + curAttr.getKey() + "\": ");
                        writer.write(curAttr.getValue());
                        if (current < size) {
                            writer.write(",");
                        }
                    }
                    prevCont = true;
                }

                // Add warnings
                if (!warnings.isEmpty()) {
                    if (prevCont) {
                        writer.write(",");
                    }
                    writer.write("\"warnings\": [");
                    for (int i = 0; i < warnings.size(); i++) {
                        writer.write("\"" + warnings.get(i) + "\"");
                        if (i < warnings.size() - 1) {
                            writer.write(",");
                        }
                    }
                    writer.write("]");
                    prevCont = true;
                }
                // Add errors
                if (!errors.isEmpty()) {
                    if (prevCont) {
                        writer.write(",");
                    }
                    writer.write("\"errors\": [");
                    for (int i = 0; i < errors.size(); i++) {
                        writer.write("\"" + errors.get(i) + "\"");
                        if (i < errors.size() - 1) {
                            writer.write(",");
                        }
                    }
                    writer.write("]");
                    prevCont = true;
                }
                // Add exceptions
                if (!exceptions.isEmpty()) {
                    if (prevCont) {
                        writer.write(",");
                    }
                    writer.write("\"exceptions\": [");
                    for (int i = 0; i < exceptions.size(); i++) {
                        writer.write("\"" + exceptions.get(i).getLocalizedMessage() + "\"");
                        if (i < exceptions.size() - 1) {
                            writer.write(",");
                        }
                    }
                    writer.write("]");
                }

                writer.write("}");
                writer.flush();
            }
        };
        rb.entity(stream);
        return rb.build();
    }

    /**
     * Generates response as String, depending on the callbackFunction set or
     * not set, it generates jsonp or json as result.
     *
     * @return Json or JsonP as result.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        boolean prevCont = false;
        if (!attrs.isEmpty()) {
            // Adding stored values
            int size = attrs.size();
            int current = 0;
            for (Entry<String, String> curAttr : this.attrs.entrySet()) {
                current++;
                sb.append("\"");
                sb.append(curAttr.getKey());
                sb.append("\": ");
                sb.append(curAttr.getValue());
                if (current < size) {
                    sb.append(",");
                }
                prevCont = true;
            }
        }

        // Add warnings
        if (!warnings.isEmpty()) {
            if (prevCont) {
                sb.append(",");
            }
            sb.append("\"warnings\": [");
            for (int i = 0; i < warnings.size(); i++) {
                sb.append("\"" + warnings.get(i) + "\"");
                if (i < warnings.size() - 1) {
                    sb.append(",");
                }
                prevCont = true;
            }
            sb.append("]");
        }
        // Add errors
        if (!errors.isEmpty()) {
            if (prevCont) {
                sb.append(",");
            }
            sb.append("\"errors\": [");
            for (int i = 0; i < errors.size(); i++) {
                sb.append("\"" + errors.get(i) + "\"");
                if (i < errors.size() - 1) {
                    sb.append(",");
                }
                prevCont = true;
            }
            sb.append("]");
        }
        // Add exceptions
        if (!exceptions.isEmpty()) {
            if (prevCont) {
                sb.append(",");
            }
            sb.append("\"exceptions\": [");
            for (int i = 0; i < exceptions.size(); i++) {
                sb.append("\"" + exceptions.get(i).getLocalizedMessage() + "\"");
                if (i < exceptions.size() - 1) {
                    sb.append(",");
                }
            }
            sb.append("]");
        }

        sb.append("}");
        return sb.toString();
    }

    /**
     * Transfers all occured errors from listbuilder to this object Note: Only
     * objects can carry error information into responses
     *
     * @param key Key wich contains the list
     * @param rlb ResponseListBuilder responsebile
     */
    private void transferErrorsFromListBuilder(String key, ResponseListBuilder rlb) {
        if (rlb.getErrors().size() > 0 || rlb.getExceptions().size() > 0) {
            this.setStatus(Status.INTERNAL_SERVER_ERROR);
            for (String curErr : rlb.getErrors()) {
                this.addErrorMessage("in key >" + key + "<: " + curErr);

            }
            for (Exception curEx : rlb.getExceptions()) {
                this.addException(curEx);
            }
        }
    }
}
