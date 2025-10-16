package com.health.multiagent.model;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Estado compartilhado entre todos os agentes do sistema.
 * Contém todas as informações do paciente e resultados de cada agente.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientAssessmentState {
    
    // Identificação e timestamp
    @JsonPropertyDescription("Unique session identifier")
    private String sessionId;
    
    @JsonPropertyDescription("Timestamp when assessment started")
    private LocalDateTime startTime;
    
    // Dados iniciais do paciente
    @JsonPropertyDescription("Patient unique identifier")
    private String patientId;
    
    @JsonPropertyDescription("Patient symptoms description")
    private String symptoms;
    
    @JsonPropertyDescription("Patient medical history")
    private String medicalHistory;
    
    @JsonPropertyDescription("List of current medications")
    @Builder.Default
    private List<String> currentMedications = new ArrayList<>();
    
    // Resultados do Triage Agent
    @JsonPropertyDescription("Risk level: LOW, MEDIUM, HIGH, CRITICAL")
    private String riskLevel;
    
    @JsonPropertyDescription("Symptoms summary from triage")
    private String symptomsSummary;
    
    @JsonPropertyDescription("Triage recommendations")
    private String triageRecommendations;
    
    // Resultados do Pharmacist Agent
    @JsonPropertyDescription("Identified drug interactions")
    @Builder.Default
    private List<String> drugInteractions = new ArrayList<>();
    
    @JsonPropertyDescription("Contraindications found")
    @Builder.Default
    private List<String> contraindications = new ArrayList<>();
    
    @JsonPropertyDescription("Pharmacist recommendations")
    private String pharmacistRecommendations;
    
    // Resultados do Exam Agent
    @JsonPropertyDescription("Recommended laboratory exams")
    @Builder.Default
    private List<String> recommendedLabExams = new ArrayList<>();
    
    @JsonPropertyDescription("Recommended imaging exams")
    @Builder.Default
    private List<String> recommendedImagingExams = new ArrayList<>();
    
    @JsonPropertyDescription("Exam priority level")
    private String examPriority;
    
    @JsonPropertyDescription("Exam recommendations rationale")
    private String examRecommendations;
    
    // Resultados do EMR/Comms Agent
    @JsonPropertyDescription("Generated FHIR document")
    private String fhirDocument;
    
    @JsonPropertyDescription("Communication text for healthcare providers")
    private String communicationText;
    
    @JsonPropertyDescription("Approval status: PENDING, APPROVED, REJECTED")
    private String approvalStatus;
    
    @JsonPropertyDescription("Comments from approver")
    private String approvalComments;
    
    // Reprocessamento
    @JsonPropertyDescription("Number of reprocessing iterations")
    @Builder.Default
    private int reprocessingCount = 0;
    
    @JsonPropertyDescription("Maximum allowed reprocessing iterations")
    @Builder.Default
    private int maxReprocessingIterations = 3;
    
    @JsonPropertyDescription("Physician feedback for reprocessing")
    private String physicianFeedback;
    
    @JsonPropertyDescription("History of previous assessments")
    @Builder.Default
    private List<String> assessmentHistory = new ArrayList<>();
    
    // Controle de fluxo
    @JsonPropertyDescription("Current agent processing")
    private String currentAgent;
    
    @JsonPropertyDescription("Overall status: PROCESSING, AWAITING_APPROVAL, COMPLETED, ERROR")
    private String status;
    
    @JsonPropertyDescription("Error message if any")
    private String errorMessage;
    
}

