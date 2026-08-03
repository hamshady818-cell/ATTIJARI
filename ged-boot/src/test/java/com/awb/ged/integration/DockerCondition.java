package com.awb.ged.integration;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.DockerClientFactory;

public class DockerCondition implements ExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        try {
            if (!DockerClientFactory.instance().isDockerAvailable()) {
                return ConditionEvaluationResult.disabled("Docker is not available");
            }
            // Verify client connectivity to detect protocol version issues early
            DockerClientFactory.instance().client();
            return ConditionEvaluationResult.enabled("Docker is available and compatible");
        } catch (Exception e) {
            return ConditionEvaluationResult.disabled("Docker is not available or incompatible: " + e.getMessage());
        }
    }
}
