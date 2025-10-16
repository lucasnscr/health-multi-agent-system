package com.health.multiagent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para resposta de avaliação
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentResponse {
    
    private String sessionId;
    
    private String status; // PROCESSING, AWAITING_APPROVAL, COMPLETED, ERROR
    
    private String currentAgent;
    
    private String message;
    
    private Object data;
    
    private InterruptionInfo interruptionInfo;
    
    /**
     * Informações sobre interrupção para aprovação humana
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InterruptionInfo {
        private String nodeId;
        private String label;
        private String type;
        private Object metadata;
    }
}

