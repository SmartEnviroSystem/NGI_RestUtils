package de.ngi.logging;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import java.time.Instant;

public class LoggerEntry {

    private final Instant timestamp;
    private final String processId;
    private final String message;
    private final String classname;
    private final String methodname;
    private final int line;

    public LoggerEntry(String processId, String message, String classname, String methodname, int line) {
        this.timestamp = Instant.now();
        this.processId = processId;
        this.message = message;
        this.classname = classname;
        this.methodname = methodname;
        this.line = line;
    }

    public JsonObject toJson() {
        var builder = Json.createObjectBuilder()
                .add("timestamp", timestamp.toString())
                .add("process", processId)
                .add("classname", classname)
                .add("methodname", methodname)
                .add("line", line);

        if (message != null && !message.isEmpty()) {
            builder.add("message", message);
        }

        return builder.build();
    }
}
