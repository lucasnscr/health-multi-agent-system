package com.health.multiagent.agent;

import com.health.multiagent.model.PatientAssessmentState;
import com.health.multiagent.model.RiskAssessment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Triage Agent - Primeiro agente do fluxo
 * Responsabilidades:
 * - Avaliar sintomas do paciente
 * - Determinar nível de risco (LOW, MEDIUM, HIGH, CRITICAL)
 * - Fornecer recomendações iniciais
 * - Identificar casos urgentes
 */
@Slf4j
@Component
public class TriageAgent {
    
    private final ChatModel chatModel;
    
    private static final String TRIAGE_PROMPT_TEMPLATE = """
        You are a medical triage agent. Analyze the patient information and provide a risk assessment.
        
        Patient Information:
        - Patient ID: {patientId}
        - Symptoms: {symptoms}
        - Medical History: {medicalHistory}
        - Current Medications: {currentMedications}
        
        {physicianFeedbackSection}
        
        Analyze the symptoms carefully and provide:
        1. Risk Level (LOW, MEDIUM, HIGH, or CRITICAL)
        2. A concise summary of the symptoms
        3. Recommended next steps
        4. Whether this case requires urgent attention (true/false)
        
        Respond in JSON format with the following structure:
        {{
          "riskLevel": "LOW|MEDIUM|HIGH|CRITICAL",
          "symptomsSummary": "brief summary",
          "recommendations": "next steps",
          "urgent": true|false
        }}
        
        Consider:
        - Severity and duration of symptoms
        - Patient's medical history
        - Potential medication interactions
        - Red flags requiring immediate attention
        
        Always prioritize patient safety.
        All content give in portuguese.
        """;
    
    public TriageAgent(ChatModel chatModel) {
        this.chatModel = chatModel;
    }
    
    /**
     * Executa a avaliação de triagem do paciente
     */
    public RiskAssessment assessPatient(PatientAssessmentState state) {
        log.info("Starting triage assessment for patient: {}", state.getPatientId());
        
        try {
            PromptTemplate promptTemplate = new PromptTemplate(TRIAGE_PROMPT_TEMPLATE);
            
            // Adicionar seção de feedback do médico se houver reprocessamento
            String feedbackSection = "";
            if (state.getPhysicianFeedback() != null && !state.getPhysicianFeedback().isEmpty()) {
                feedbackSection = String.format("""
                    
                    IMPORTANT - Physician Feedback from Previous Assessment:
                    %s
                    
                    Please incorporate this feedback in your new assessment.
                    Reprocessing iteration: %d of %d
                    """, 
                    state.getPhysicianFeedback(),
                    state.getReprocessingCount(),
                    state.getMaxReprocessingIterations()
                );
            }
            
            Map<String, Object> variables = Map.of(
                "patientId", state.getPatientId() != null ? state.getPatientId() : "UNKNOWN",
                "symptoms", state.getSymptoms() != null ? state.getSymptoms() : "No symptoms provided",
                "medicalHistory", state.getMedicalHistory() != null ? state.getMedicalHistory() : "No history available",
                "currentMedications", state.getCurrentMedications() != null && !state.getCurrentMedications().isEmpty() 
                    ? String.join(", ", state.getCurrentMedications()) 
                    : "None reported",
                "physicianFeedbackSection", feedbackSection
            );
            
            Prompt prompt = promptTemplate.create(variables);
            String response = chatModel.call(prompt).getResult().getOutput().getText();
            
            log.debug("Triage agent response: {}", response);
            
            // Parse JSON response (simplified - in production use Jackson ObjectMapper)
            if (response == null || response.isEmpty()) {
                throw new RuntimeException("Empty response from exam agent");
            }
            RiskAssessment assessment = parseRiskAssessment(response);
            
            log.info("Triage completed - Risk Level: {}, Urgent: {}", 
                assessment.riskLevel(), assessment.urgent());
            
            return assessment;
            
        } catch (Exception e) {
            log.error("Error during triage assessment", e);
            // Return safe default assessment
            return new RiskAssessment(
                "MEDIUM",
                "Error during assessment: " + e.getMessage(),
                "Manual review required due to system error",
                true
            );
        }
    }
    
    /**
     * Parse JSON response to RiskAssessment object
     * Simplified version - in production use Jackson ObjectMapper
     */
    private RiskAssessment parseRiskAssessment(String jsonResponse) {
        // Remove markdown code blocks if present
        String cleaned = jsonResponse.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
        
        // Simple parsing (in production, use Jackson)
        String riskLevel = extractJsonValue(cleaned, "riskLevel", "MEDIUM");
        String summary = extractJsonValue(cleaned, "symptomsSummary", "Assessment completed");
        String recommendations = extractJsonValue(cleaned, "recommendations", "Proceed to next evaluation");
        boolean urgent = Boolean.parseBoolean(extractJsonValue(cleaned, "urgent", "false"));
        
        return new RiskAssessment(riskLevel, summary, recommendations, urgent);
    }
    
    /**
     * Extract value from JSON string (simplified)
     */
    private String extractJsonValue(String json, String key, String defaultValue) {
        try {
            String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]+)\"";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(json);
            if (m.find()) {
                return m.group(1);
            }
            
            // Try boolean/number pattern
            pattern = "\"" + key + "\"\\s*:\\s*([^,}\\s]+)";
            p = java.util.regex.Pattern.compile(pattern);
            m = p.matcher(json);
            if (m.find()) {
                return m.group(1);
            }
        } catch (Exception e) {
            log.warn("Error extracting {} from JSON, using default", key, e);
        }
        return defaultValue;
    }
}

