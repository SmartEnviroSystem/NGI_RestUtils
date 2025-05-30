package de.ngi.logging;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import java.util.*;
import java.util.function.Consumer;

public class Logger {
    private static final ThreadLocal<String> PROCESS_ID = new ThreadLocal<>();
    private static final ThreadLocal<List<LoggerEntry>> LOG_ENTRIES = ThreadLocal.withInitial(ArrayList::new);
    private static final List<Consumer<String>> logConsumers = new ArrayList<>();

    public static void startProcess() {
        startProcess(UUID.randomUUID().toString());
    }
    
    public static void startProcess(String processId) {
        PROCESS_ID.set(processId);
        log("Started process: " + processId,4);
    }

    public static void log() {
        log(null,3);
    }
    
    public static void log(String message) {
        // stackDepth = 3 => Caller of this method. Method calls: Caller -> log(message) -> log(message, stackDepth) -> getStackTrace()
        log(message, 3);
    }
    
    public static void log(String message, int stackDepth) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        // Index 0 ist getStackTrace(), Index 1 ist diese Methode selbst, Index 2 ist der Aufrufer
        if (stackTrace.length > 3) {
            LoggerEntry logEntry = new LoggerEntry(
                    PROCESS_ID.get(),
                    message,
                    stackTrace[stackDepth].getClassName(),
                    stackTrace[stackDepth].getMethodName(),
                    stackTrace[stackDepth].getLineNumber()
            );
            LOG_ENTRIES.get().add(logEntry);
            logConsumers.forEach(consumer -> consumer.accept(logEntry.toString()));
        }
    }

    public static void endProcess() {
        log("Process finished: " + PROCESS_ID.get());
        System.out.println("Logged for process " + PROCESS_ID.get() + ":");
        JsonArrayBuilder jab = Json.createArrayBuilder();
        for(LoggerEntry curEntry : LOG_ENTRIES.get()) {
            jab.add(curEntry.toJson());
        }
        
        System.out.println(jab.build().toString());

        // Bereinige ThreadLocals
        PROCESS_ID.remove();
        LOG_ENTRIES.remove();
    }

    public static void addLogConsumer(Consumer<String> consumer) {
        logConsumers.add(consumer);
    }
}
