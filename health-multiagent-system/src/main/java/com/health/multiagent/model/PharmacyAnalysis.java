package com.health.multiagent.model;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.List;

/**
 * Resultado da análise farmacêutica do Pharmacist Agent
 */
public record PharmacyAnalysis(
    @JsonPropertyDescription("List of identified drug interactions")
    List<String> drugInteractions,
    
    @JsonPropertyDescription("List of contraindications found")
    List<String> contraindications,
    
    @JsonPropertyDescription("Medication recommendations and adjustments")
    String recommendations,
    
    @JsonPropertyDescription("Safety concerns: true if critical issues found")
    boolean hasSafetyConcerns
) {}

