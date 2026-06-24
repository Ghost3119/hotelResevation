package com.hotelmanager;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.DockerClientFactory;

/**
 * Condicion de JUnit 5 que habilita las pruebas solo cuando Testcontainers
 * detecta un demonio Docker alcanzable. Se evalua ANTES de que el
 * {@code @Container} estatico intente arrancar, por lo que sin Docker las
 * pruebas se omiten (skip) en lugar de fallar con
 * "Could not find a valid Docker environment".
 */
public class DockerAvailableCondition implements ExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        try {
            boolean available = DockerClientFactory.instance().isDockerAvailable();
            if (available) {
                return ConditionEvaluationResult.enabled("Docker daemon is available");
            }
            return ConditionEvaluationResult.disabled(
                    "Docker daemon not available - Testcontainers integration tests skipped");
        } catch (Throwable t) {
            return ConditionEvaluationResult.disabled(
                    "Docker daemon not reachable (" + safeMessage(t)
                            + ") - Testcontainers integration tests skipped");
        }
    }

    private static String safeMessage(Throwable t) {
        String m = t.getMessage();
        return m == null ? t.getClass().getSimpleName() : m;
    }
}
