package de.ngi.restutils;

import java.util.function.Consumer;

public class LoggingWebSocketConsumer implements Consumer<String> {
    @Override
    public void accept(String logMessage) {
        LoggingWebSocketServer.sendLog(logMessage);
    }
}
