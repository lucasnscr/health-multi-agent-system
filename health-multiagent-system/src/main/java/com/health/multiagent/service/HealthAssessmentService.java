package com.health.multiagent.service;

import com.health.multiagent.agent.EMRCommsAgent;
import com.health.multiagent.agent.ExamAgent;
import com.health.multiagent.agent.PharmacistAgent;
import com.health.multiagent.agent.TriageAgent;
import com.health.multiagent.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serviço orquestrador do fluxo multi-agente
 * Coordena a execução sequencial dos agentes:
 * 1. Triage Agent
 * 2. Pharmacist Agent
 * 3. Exam Agent
 * 4. EMR/Comms Agent (com aprovação humana)
 */
@Slf4j
@Service
public class HealthAssessmentService {
    
    private final TriageAgent triageAgent;
    private final PharmacistAgent pharmacistAgent;
    private final ExamAgent examAgent;
    private final EMRCommsAgent emrCommsAgent;
    
    // Armazena sessões ativas (em produção, usar banco de dados)
    private final Map<String, PatientAssessmentState> activeSessions = new ConcurrentHashMap<>();
    
    public HealthAssessmentService(
            TriageAgent triageAgent,
            PharmacistAgent pharmacistAgent,
            ExamAgent examAgent,
            EMRCommsAgent emrCommsAgent) {
        this.triageAgent = triageAgent;
        this.pharmacistAgent = pharmacistAgent;
        this.examAgent = examAgent;
        this.emrCommsAgent = emrCommsAgent;
    }
    
    /**
     * Inicia uma nova avaliação de paciente
     */
    public PatientAssessmentState startAssessment(PatientAssessmentState initialState) {
        log.info("Starting new patient assessment");
        
        // Gerar ID de sessão
        String sessionId = UUID.randomUUID().toString();
        initialState.setSessionId(sessionId);
        initialState.setStartTime(LocalDateTime.now());
        initialState.setStatus("PROCESSING");
        
        // Salvar sessão
        activeSessions.put(sessionId, initialState);
        
        try {
            // Executar fluxo de agentes
            executeAgentFlow(initialState);
            
            return initialState;
            
        } catch (Exception e) {
            log.error("Error during assessment", e);
            initialState.setStatus("ERROR");
            initialState.setErrorMessage(e.getMessage());
            return initialState;
        }
    }
    
    /**
     * Executa o fluxo sequencial de agentes
     */
    private void executeAgentFlow(PatientAssessmentState state) {
        log.info("Executing agent flow for session: {}", state.getSessionId());
        
        // 1. Triage Agent
        state.setCurrentAgent("TRIAGE");
        RiskAssessment riskAssessment = triageAgent.assessPatient(state);
        state.setRiskLevel(riskAssessment.riskLevel());
        state.setSymptomsSummary(riskAssessment.symptomsSummary());
        state.setTriageRecommendations(riskAssessment.recommendations());
        
        log.info("Triage completed - Risk: {}", riskAssessment.riskLevel());
        
        // 2. Pharmacist Agent
        state.setCurrentAgent("PHARMACIST");
        PharmacyAnalysis pharmacyAnalysis = pharmacistAgent.analyzeMedications(state);
        state.setDrugInteractions(pharmacyAnalysis.drugInteractions());
        state.setContraindications(pharmacyAnalysis.contraindications());
        state.setPharmacistRecommendations(pharmacyAnalysis.recommendations());
        
        log.info("Pharmacy analysis completed - Interactions: {}", 
            pharmacyAnalysis.drugInteractions().size());
        
        // 3. Exam Agent
        state.setCurrentAgent("EXAM");
        ExamRecommendations examRecommendations = examAgent.recommendExams(state);
        state.setRecommendedLabExams(examRecommendations.laboratoryExams());
        state.setRecommendedImagingExams(examRecommendations.imagingExams());
        state.setExamPriority(examRecommendations.priority());
        state.setExamRecommendations(examRecommendations.rationale());
        
        log.info("Exam recommendations completed - Priority: {}", examRecommendations.priority());
        
        // 4. EMR/Comms Agent
        state.setCurrentAgent("EMR_COMMS");
        FHIRDocumentation documentation = emrCommsAgent.generateDocumentation(state);
        state.setFhirDocument(documentation.fhirDocument());
        state.setCommunicationText(documentation.communicationText());
        
        log.info("FHIR documentation generated - Type: {}", documentation.documentType());
        
        // Aguardar aprovação humana
        state.setStatus("AWAITING_APPROVAL");
        state.setApprovalStatus("PENDING");
        
        log.info("Assessment completed, awaiting human approval");
    }
    
    /**
     * Processa aprovação humana
     */
    public PatientAssessmentState processApproval(String sessionId, String decision, String comments) {
        log.info("Processing approval for session: {} - Decision: {}", sessionId, decision);
        
        PatientAssessmentState state = activeSessions.get(sessionId);
        if (state == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }
        
        if (!"AWAITING_APPROVAL".equals(state.getStatus())) {
            throw new IllegalStateException("Session is not awaiting approval");
        }
        
        state.setApprovalStatus(decision);
        state.setApprovalComments(comments);
        
        if ("APPROVED".equals(decision)) {
            state.setStatus("COMPLETED");
            log.info("Assessment approved and completed for session: {}", sessionId);
        } else if ("REJECTED".equals(decision)) {
            // Verificar se pode reprocessar
            if (state.getReprocessingCount() < state.getMaxReprocessingIterations()) {
                log.info("Assessment rejected, initiating reprocessing. Iteration: {}", 
                    state.getReprocessingCount() + 1);
                
                // Salvar feedback do médico
                state.setPhysicianFeedback(comments);
                
                // Salvar estado atual no histórico
                saveCurrentStateToHistory(state);
                
                // Incrementar contador de reprocessamento
                state.setReprocessingCount(state.getReprocessingCount() + 1);
                
                // Reprocessar com feedback
                reprocessWithFeedback(state);
                
            } else {
                state.setStatus("REJECTED");
                log.warn("Maximum reprocessing iterations reached for session: {}", sessionId);
            }
        }
        
        return state;
    }
    
    /**
     * Salva o estado atual no histórico antes de reprocessar
     */
    private void saveCurrentStateToHistory(PatientAssessmentState state) {
        String historyEntry = String.format(
            "Iteration %d - Risk: %s, Exams: %s, Physician Feedback: %s",
            state.getReprocessingCount(),
            state.getRiskLevel(),
            state.getRecommendedLabExams() != null ? state.getRecommendedLabExams().size() : 0,
            state.getPhysicianFeedback()
        );
        state.getAssessmentHistory().add(historyEntry);
        log.debug("Saved to history: {}", historyEntry);
    }
    
    /**
     * Reprocessa a avaliação incorporando o feedback do médico
     */
    private void reprocessWithFeedback(PatientAssessmentState state) {
        log.info("Reprocessing assessment with physician feedback for session: {}", 
            state.getSessionId());
        
        try {
            // Limpar resultados anteriores (mantendo dados iniciais do paciente)
            clearPreviousResults(state);
            
            // Reexecutar fluxo de agentes com feedback incorporado
            state.setStatus("REPROCESSING");
            executeAgentFlow(state);
            
            log.info("Reprocessing completed for session: {}", state.getSessionId());
            
        } catch (Exception e) {
            log.error("Error during reprocessing", e);
            state.setStatus("ERROR");
            state.setErrorMessage("Reprocessing failed: " + e.getMessage());
        }
    }
    
    /**
     * Limpa resultados anteriores mantendo dados iniciais do paciente
     */
    private void clearPreviousResults(PatientAssessmentState state) {
        // Manter: patientId, symptoms, medicalHistory, currentMedications, physicianFeedback
        // Limpar: resultados dos agentes
        state.setRiskLevel(null);
        state.setSymptomsSummary(null);
        state.setTriageRecommendations(null);
        state.setDrugInteractions(new ArrayList<>());
        state.setContraindications(new ArrayList<>());
        state.setPharmacistRecommendations(null);
        state.setRecommendedLabExams(new ArrayList<>());
        state.setRecommendedImagingExams(new ArrayList<>());
        state.setExamPriority(null);
        state.setExamRecommendations(null);
        state.setFhirDocument(null);
        state.setCommunicationText(null);
        state.setApprovalStatus("PENDING");
        
        log.debug("Cleared previous results for reprocessing");
    }
    
    /**
     * Recupera estado de uma sessão
     */
    public PatientAssessmentState getSessionState(String sessionId) {
        PatientAssessmentState state = activeSessions.get(sessionId);
        if (state == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }
        return state;
    }
    
    /**
     * Remove sessão (após conclusão ou timeout)
     */
    public void removeSession(String sessionId) {
        activeSessions.remove(sessionId);
        log.info("Session removed: {}", sessionId);
    }
}

