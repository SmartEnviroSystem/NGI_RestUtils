package de.fhbielefeld.scl.rest.dataflow;


/**
 * Interceptor for logging data flows
 *
 * @author Florian Fehring
 */
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.ws.rs.Path;
import jakarta.websocket.Session;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Interceptor
@DataFlowLogging
public class DataFlowInterceptor {

    private static Session session;
    private static final String sessionId = UUID.randomUUID().toString(); // Eindeutige ID pro Service

    private static List<String> messages = new ArrayList<>();

    //TODO build websocket server
//    static {
//        try {
//            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
//            session = container.connectToServer(ClientEndpoint.class, new URI("ws://localhost:8080/SmartDataFlow/log"));
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
    @AroundInvoke
    public Object logMethodCall(InvocationContext context) throws Exception {
        Method method = context.getMethod();
        boolean isRestMethod = method.isAnnotationPresent(Path.class);

        // Methode betreten -> Log senden
        sendLog("Start: " + method.getName());
        System.out.println("TEST SmartDataFlow: start " + method.getName());

        Object result = context.proceed(); // Methode ausführen

        // Methode verlassen -> Diagramm zurücksetzen, wenn es eine REST-Methode war
        if (isRestMethod) {
            sendLog("RESET");
            System.out.println("TEST SmartDataFlow: endREST-API call: " + method.getName());
        }

        return result;
    }

    public static void log() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        // Index 0 ist getStackTrace(), Index 1 ist diese Methode selbst, Index 2 ist der Aufrufer
        if (stackTrace.length > 2) {
            String aufruferKlasse = stackTrace[2].getClassName();
            String aufruferMethode = stackTrace[2].getMethodName();
            messages.add(aufruferKlasse+"."+aufruferMethode+"()");
//            System.out.println("Meine Methode wurde aufgerufen von: " + aufruferKlasse + "." + aufruferMethode);
        }
    }
    
    public static void log(String msg) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        // Index 0 ist getStackTrace(), Index 1 ist diese Methode selbst, Index 2 ist der Aufrufer
        if (stackTrace.length > 2) {
            String aufruferKlasse = stackTrace[2].getClassName();
            String aufruferMethode = stackTrace[2].getMethodName();
            messages.add(sessionId + ":" + aufruferKlasse+"."+aufruferMethode+"(: " + msg);
//            System.out.println("Meine Methode wurde aufgerufen von: " + aufruferKlasse + "." + aufruferMethode);
        }
    }

    public static void sendLog(String message) {
        String fullMessage = sessionId + ":" + message; // Session-ID vor die Nachricht setzen
        System.out.println("DataFlowLogging: " + fullMessage);
        messages.add(fullMessage);
        if (session != null && session.isOpen()) {
            session.getAsyncRemote().sendText(fullMessage);
        }
    }

    public static void close() {
        System.out.println("Closeing");
        System.out.println(messages);
        messages = new ArrayList<>();
    }
}
