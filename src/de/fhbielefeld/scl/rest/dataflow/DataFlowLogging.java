package de.fhbielefeld.scl.rest.dataflow;

import jakarta.interceptor.InterceptorBinding;
import java.lang.annotation.*;

/**
 * Annotation for DataFlowLogging and visualisation
 * 
 * @author Florian Fehring
 */
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface DataFlowLogging {
    String value() default ""; // Optionaler Name für bessere Lesbarkeit
}