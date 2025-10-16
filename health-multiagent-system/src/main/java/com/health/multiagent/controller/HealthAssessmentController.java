package com.health.multiagent.controller;

import com.health.multiagent.model.*;
import com.health.multiagent.service.HealthAssessmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller para o sistema multi-agente de saúde
 * Endpoints:
 * - POST /symptoms - Submeter sintomas e iniciar avaliação
 * - POST /approve/{sessionId} - Aprovar/rejeitar ações do agente
 * - GET /status/{sessionId} - Verificar status da avaliação
 */
@Slf4j
@RestController
@RequestMapping("/health-assessment")
@Tag(name = "Health Assessment", description = "Multi-agent health assessment system API")
@CrossOrigin(origins = "http://localhost:5173")
public class HealthAssessmentController {
    
    private final HealthAssessmentService assessmentService;
    
    public HealthAssessmentController(HealthAssessmentService assessmentService) {
        this.assessmentService = assessmentService;
    }
    
    /**
     * Endpoint para submissão de sintomas e início da avaliação
     */
    @PostMapping("/symptoms")
    @Operation(
        summary = "Submit patient symptoms",
        description = "Initiates a multi-agent assessment flow based on patient symptoms"
    )
    public ResponseEntity<AssessmentResponse> submitSymptoms(
            @Valid @RequestBody SymptomsRequest request) {
        
        log.info("Received symptoms submission for patient: {}", request.getPatientId());
        
        try {
            // Converter request para estado inicial
            PatientAssessmentState initialState = PatientAssessmentState.builder()
                .patientId(request.getPatientId())
                .symptoms(request.getSymptoms())
                .medicalHistory(request.getMedicalHistory())
                .currentMedications(request.getCurrentMedications())
                .build();
            
            // Iniciar avaliação
            PatientAssessmentState result = assessmentService.startAssessment(initialState);
            
            // Construir resposta
            AssessmentResponse response = buildResponse(result);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error processing symptoms submission", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(AssessmentResponse.builder()
                    .status("ERROR")
                    .message("Error processing request: " + e.getMessage())
                    .build());
        }
    }
    
    /**
     * Endpoint para aprovação/rejeição de ações
     */
    @PostMapping("/approve/{sessionId}")
    @Operation(
        summary = "Approve or reject agent actions",
        description = "Process human approval for FHIR documentation and communications"
    )
    public ResponseEntity<AssessmentResponse> approveAction(
            @Parameter(description = "Session ID") @PathVariable String sessionId,
            @Valid @RequestBody ApprovalRequest approval) {
        
        log.info("Received approval for session: {} - Decision: {}", 
            sessionId, approval.getDecision());
        
        try {
            PatientAssessmentState result = assessmentService.processApproval(
                sessionId, 
                approval.getDecision(), 
                approval.getComments()
            );
            
            AssessmentResponse response = buildResponse(result);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid session ID: {}", sessionId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(AssessmentResponse.builder()
                    .status("ERROR")
                    .message("Session not found: " + sessionId)
                    .build());
                    
        } catch (IllegalStateException e) {
            log.error("Invalid state for approval", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(AssessmentResponse.builder()
                    .status("ERROR")
                    .message(e.getMessage())
                    .build());
                    
        } catch (Exception e) {
            log.error("Error processing approval", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(AssessmentResponse.builder()
                    .status("ERROR")
                    .message("Error processing approval: " + e.getMessage())
                    .build());
        }
    }
    
    /**
     * Endpoint para verificar status da avaliação
     */
    @GetMapping("/status/{sessionId}")
    @Operation(
        summary = "Get assessment status",
        description = "Retrieve current status and results of an assessment session"
    )
    public ResponseEntity<AssessmentResponse> getStatus(
            @Parameter(description = "Session ID") @PathVariable String sessionId) {
        
        log.info("Status check for session: {}", sessionId);
        
        try {
            PatientAssessmentState state = assessmentService.getSessionState(sessionId);
            AssessmentResponse response = buildResponse(state);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.error("Session not found: {}", sessionId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(AssessmentResponse.builder()
                    .status("ERROR")
                    .message("Session not found: " + sessionId)
                    .build());
                    
        } catch (Exception e) {
            log.error("Error retrieving status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(AssessmentResponse.builder()
                    .status("ERROR")
                    .message("Error retrieving status: " + e.getMessage())
                    .build());
        }
    }
    
    /**
     * Constrói resposta a partir do estado
     */
    private AssessmentResponse buildResponse(PatientAssessmentState state) {
        AssessmentResponse.AssessmentResponseBuilder builder = AssessmentResponse.builder()
            .sessionId(state.getSessionId())
            .status(state.getStatus())
            .currentAgent(state.getCurrentAgent());
        
        // Mensagem baseada no status
        switch (state.getStatus()) {
            case "PROCESSING":
                builder.message("Assessment in progress. Current agent: " + state.getCurrentAgent());
                break;
            case "REPROCESSING":
                builder.message(String.format(
                    "Reprocessing assessment based on physician feedback. Iteration %d of %d.",
                    state.getReprocessingCount(),
                    state.getMaxReprocessingIterations()
                ));
                break;
            case "AWAITING_APPROVAL":
                String approvalMessage = "Assessment completed. Awaiting human approval for FHIR documentation.";
                if (state.getReprocessingCount() > 0) {
                    approvalMessage = String.format(
                        "Reprocessed assessment ready (iteration %d). Please review and approve or provide additional feedback.",
                        state.getReprocessingCount()
                    );
                }
                builder.message(approvalMessage);
                builder.interruptionInfo(AssessmentResponse.InterruptionInfo.builder()
                    .nodeId("emr_comms_approval")
                    .label("Approve FHIR documentation and communications?")
                    .type("fhir_approval")
                    .metadata(state)
                    .build());
                break;
            case "COMPLETED":
                String completedMessage = "Assessment completed and approved.";
                if (state.getReprocessingCount() > 0) {
                    completedMessage = String.format(
                        "Assessment completed and approved after %d reprocessing iteration(s).",
                        state.getReprocessingCount()
                    );
                }
                builder.message(completedMessage);
                break;
            case "REJECTED":
                String rejectedMessage = "Assessment rejected by approver.";
                if (state.getReprocessingCount() >= state.getMaxReprocessingIterations()) {
                    rejectedMessage = String.format(
                        "Assessment rejected. Maximum reprocessing iterations (%d) reached.",
                        state.getMaxReprocessingIterations()
                    );
                }
                builder.message(rejectedMessage);
                break;
            case "ERROR":
                builder.message("Error during assessment: " + state.getErrorMessage());
                break;
            default:
                builder.message("Unknown status");
        }
        
        // Adicionar dados completos do estado
        builder.data(state);
        
        return builder.build();
    }
}

