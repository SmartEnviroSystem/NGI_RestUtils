package de.fhbielefeld.scl.rest.util;

import de.fhbielefeld.scl.logger.Logger;
import de.fhbielefeld.scl.logger.message.Message;
import de.fhbielefeld.scl.logger.message.MessageLevel;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;

/**
 * ResponseBuilder shared functionality for ListResponseBuilder and
 * ObjectResponseBuilder
 *
 * @author Florian Fehring
 */
public abstract class ApiResponseBuilder {

    private static boolean debugmode = false;
    private Response.Status status = null;
    private NewCookie cookie = null;
    private String downloadFileName = null;

    private final Map<String, Class> convertedToString = new HashMap<>();
    private final List<String> nullfields = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();
    private final List<Throwable> exceptions = new ArrayList<>();

    /**
     * Sets the state of the debugmode
     *
     * @param debug true for enabling
     * @return
     */
    public ApiResponseBuilder setDebugMode(boolean debug) {
        debugmode = debug;
        return this;
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
    public ApiResponseBuilder setStatus(Response.Status status) {
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
    public ApiResponseBuilder setCookie(String name, String value, int maxAge) {
        // Defines the cookie without domain, path and comment. Send over http and https and without httpOnly mode (allow access with javascript)
        this.cookie = new NewCookie(name, value, "/", null, null, maxAge, false, false);
        return this;
    }

    /**
     * Declares the generated response as downloadable.The given filename will
     * be the suggested filename for download in client.
     *
     * @param filename
     * @return
     */
    public ApiResponseBuilder setDownloadFileName(String filename) {
        this.downloadFileName = filename;
        return this;
    }

    public ApiResponseBuilder addConvertedToString(String key, Class convclass) {
        this.convertedToString.put(key, convclass);
        return this;
    }

    public Map<String, Class> getConvertedToString() {
        return convertedToString;
    }

    public ApiResponseBuilder addNullField(String nullfield) {
        this.nullfields.add(nullfield);
        return this;
    }

    public List<String> getNullfields() {
        return nullfields;
    }

    public ApiResponseBuilder addWarningMessage(String warningMessage) {
        this.warnings.add(warningMessage);
        return this;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public ApiResponseBuilder addErrorMessage(String errorMessage) {
        this.errors.add(errorMessage);
        return this;
    }

    public List<String> getErrors() {
        return errors;
    }

    /**
     * Adds an exception that should be delivered in the response (within debug
     * mode only)
     *
     * @param ex Exception to deliver
     * @return The modified responsebuilder
     */
    public ApiResponseBuilder addException(Throwable ex) {
        this.exceptions.add(ex);
        return this;
    }

    public List<Throwable> getExceptions() {
        return exceptions;
    }

    public ApiResponseBuilder mergeMessages(ApiResponseBuilder arb) {
        this.convertedToString.putAll(arb.convertedToString);
        this.nullfields.addAll(arb.nullfields);
        this.warnings.addAll(arb.warnings);
        this.errors.addAll(arb.errors);
        this.exceptions.addAll(arb.exceptions);
        if (!this.errors.isEmpty() || !this.exceptions.isEmpty()) {
            this.setStatus(Response.Status.INTERNAL_SERVER_ERROR);
        }
        return this;
    }

    /**
     * Creates an response object as stated in jaxws.rs standard
     *
     * @return
     */
    public Response toResponse() {
        ResponseBuilder rb = this.createResponseBuilder();
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

        StreamingOutput stream = new StreamingOutput() {
            @Override
            public void write(OutputStream out) throws IOException, WebApplicationException {
                Writer writer = new BufferedWriter(new OutputStreamWriter(out));
                writer.write("{");

                boolean prevCont = toWriter(writer);

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
        ResponseBuilder rb = this.createResponseBuilder();
        rb.entity(stream);
        return rb.build();
    }

    /**
     * Create a JAX-RS ResponseBuilder for this ApiResponseBuilder Adds header
     * information from settings and data inside the ApiResponeBuilder
     */
    private ResponseBuilder createResponseBuilder() {        
        if (this.status == null) {
            this.status = Response.Status.INTERNAL_SERVER_ERROR;
            this.addErrorMessage("There was no status set for this response");
        }
        Response.ResponseBuilder rb = Response.status(this.status);
        if (this.cookie != null) {
            rb.cookie(this.cookie);
        }
        if (this.downloadFileName != null) {
            rb.header("Content-Disposition", "attachment; filename=" + this.downloadFileName);
        }

        if (!this.getConvertedToString().isEmpty()) {
            String convertedMapStr = "Values for [";
            for (Map.Entry<String, Class> curConv : this.getConvertedToString().entrySet()) {
                convertedMapStr += curConv.getKey() + "(" + curConv.getValue().getSimpleName() + "), ";
            }
            convertedMapStr += "] converted to string representation.";
            Message msg = new Message(convertedMapStr, MessageLevel.WARNING);
            Logger.addDebugMessage(msg);
            this.addWarningMessage(convertedMapStr);
        }

        // Debug message for null fields
        if (!this.getNullfields().isEmpty()) {
            Message msg = new Message(
                    "The fields >" + this.getNullfields() + "< are not included in result, because they have null values.",
                    MessageLevel.INFO);
            Logger.addDebugMessage(msg);
        }

        return rb;
    }

    /**
     * Generates response as json
     *
     * @return Json or JsonP as result.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        // Add content
        String json = this.toJson();
        sb.append(json);
        boolean prevCont = !json.isEmpty();

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
     * Converts the ApiResponse into JSON representation
     *
     * @return ApiResponse JSON representation
     */
    public abstract String toJson();

    /**
     * Writes the ApiResponse into an Writer object
     *
     * @param writer Writer where to write the contents
     * @return True if content was written
     */
    public abstract boolean toWriter(Writer writer) throws IOException;
}
