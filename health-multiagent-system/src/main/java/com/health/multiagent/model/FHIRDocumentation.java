package com.health.multiagent.model;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Resultado da geração de documentação FHIR do EMR/Comms Agent
 */
public record FHIRDocumentation(
    @JsonPropertyDescription("FHIR formatted document in JSON")
    String fhirDocument,
    
    @JsonPropertyDescription("Communication text for healthcare providers")
    String communicationText,
    
    @JsonPropertyDescription("Document type: ASSESSMENT, REFERRAL, PRESCRIPTION")
    String documentType
) {}

