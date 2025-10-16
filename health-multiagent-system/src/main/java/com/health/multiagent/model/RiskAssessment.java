package com.health.multiagent.model;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Resultado da avaliação de risco do Triage Agent
 */
public record RiskAssessment(
    @JsonPropertyDescription("Risk level: LOW, MEDIUM, HIGH, CRITICAL")
    String riskLevel,
    
    @JsonPropertyDescription("Summary of patient symptoms")
    String symptomsSummary,
    
    @JsonPropertyDescription("Recommended next steps")
    String recommendations,
    
    @JsonPropertyDescription("Urgency indicator: true if immediate attention needed")
    boolean urgent
) {}

