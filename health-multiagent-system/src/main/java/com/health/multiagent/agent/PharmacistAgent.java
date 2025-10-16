package com.health.multiagent.agent;

import com.health.multiagent.model.PatientAssessmentState;
import com.health.multiagent.model.PharmacyAnalysis;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Pharmacist Agent - Segundo agente do fluxo
 * Responsabilidades:
 * - Analisar medicações atuais do paciente
 * - Identificar interações medicamentosas
 * - Detectar contraindicações
 * - Recomendar ajustes de medicação
 */
@Slf4j
@Component
public class PharmacistAgent {
    
    private final ChatModel chatModel;
    
    private static final String PHARMACY_PROMPT_TEMPLATE = """
        You are a pharmacist agent specialized in medication analysis and drug interactions.
        
        Patient Information:
        - Patient ID: {patientId}
        - Symptoms: {symptoms}
        - Medical History: {medicalHistory}
        - Current Medications: {currentMedications}
        - Risk Level from Triage: {riskLevel}
        - Triage Recommendations: {triageRecommendations}
        
        {physicianFeedbackSection}
        
        Analyze the patient's medications and provide:
        1. List of potential drug interactions (if any)
        2. List of contraindications based on symptoms and history
        3. Medication recommendations and adjustments
        4. Whether there are critical safety concerns (true/false)
        
        Respond in JSON format with the following structure:
        {{
          "drugInteractions": ["interaction1", "interaction2"],
          "contraindications": ["contraindication1", "contraindication2"],
          "recommendations": "detailed recommendations",
          "hasSafetyConcerns": true|false
        }}
        
        Consider:
        - Drug-drug interactions
        - Drug-disease interactions
        - Dosage appropriateness
        - Potential side effects related to current symptoms
        - Age-related considerations
        
        If no medications are reported, focus on medication recommendations based on symptoms.
        Always prioritize patient safety.
        All content give in portuguese.
        """;
    
    public PharmacistAgent(ChatModel chatModel) {
        this.chatModel = chatModel;
    }
    
    /**
     * Executa análise farmacêutica
     */
    public PharmacyAnalysis analyzeMedications(PatientAssessmentState state) {
        log.info("Starting pharmacy analysis for patient: {}", state.getPatientId());
        
        try {
            PromptTemplate promptTemplate = new PromptTemplate(PHARMACY_PROMPT_TEMPLATE);
            
            // Adicionar seção de feedback do médico se houver reprocessamento
            String feedbackSection = "";
            if (state.getPhysicianFeedback() != null && !state.getPhysicianFeedback().isEmpty()) {
                feedbackSection = String.format("""
                    
                    IMPORTANT - Physician Feedback from Previous Assessment:
                    %s
                    
                    Please incorporate this feedback in your medication analysis.
                    Reprocessing iteration: %d of %d
                    """, 
                    state.getPhysicianFeedback(),
                    state.getReprocessingCount(),
                    state.getMaxReprocessingIterations()
                );
            }
            
            Map<String, Object> variables = Map.of(
                "patientId", state.getPatientId() != null ? state.getPatientId() : "UNKNOWN",
                "symptoms", state.getSymptoms() != null ? state.getSymptoms() : "No symptoms",
                "medicalHistory", state.getMedicalHistory() != null ? state.getMedicalHistory() : "No history",
                "currentMedications", state.getCurrentMedications() != null && !state.getCurrentMedications().isEmpty()
                    ? String.join(", ", state.getCurrentMedications())
                    : "None reported",
                "riskLevel", state.getRiskLevel() != null ? state.getRiskLevel() : "UNKNOWN",
                "triageRecommendations", state.getTriageRecommendations() != null 
                    ? state.getTriageRecommendations() 
                    : "No recommendations",
                "physicianFeedbackSection", feedbackSection
            );
            
            Prompt prompt = promptTemplate.create(variables);
            String response = chatModel.call(prompt).getResult().getOutput().getText();
            
            log.debug("Pharmacist agent response: {}", response);

            if (response == null || response.isEmpty()) {
                throw new RuntimeException("Empty response from exam agent");
            }
            PharmacyAnalysis analysis = parsePharmacyAnalysis(response);
            
            log.info("Pharmacy analysis completed - Safety Concerns: {}, Interactions: {}", 
                analysis.hasSafetyConcerns(), analysis.drugInteractions().size());
            
            return analysis;
            
        } catch (Exception e) {
            log.error("Error during pharmacy analysis", e);
            return new PharmacyAnalysis(
                List.of(),
                List.of(),
                "Error during analysis: " + e.getMessage() + ". Manual pharmacy review required.",
                true
            );
        }
    }
    
    /**
     * Parse JSON response to PharmacyAnalysis object
     */
    private PharmacyAnalysis parsePharmacyAnalysis(String jsonResponse) {
        String cleaned = jsonResponse.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
        
        List<String> interactions = extractJsonArray(cleaned, "drugInteractions");
        List<String> contraindications = extractJsonArray(cleaned, "contraindications");
        String recommendations = extractJsonValue(cleaned, "recommendations", "No specific recommendations");
        boolean safetyConcerns = Boolean.parseBoolean(extractJsonValue(cleaned, "hasSafetyConcerns", "false"));
        
        return new PharmacyAnalysis(interactions, contraindications, recommendations, safetyConcerns);
    }
    
    /**
     * Extract array from JSON string (simplified)
     */
    private List<String> extractJsonArray(String json, String key) {
        try {
            String pattern = "\"" + key + "\"\\s*:\\s*\\[([^]]+)]";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(json);
            if (m.find()) {
                String arrayContent = m.group(1);
                return Arrays.stream(arrayContent.split(","))
                    .map(s -> s.trim().replaceAll("^\"|\"$", ""))
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("Error extracting array {} from JSON", key, e);
        }
        return new ArrayList<>();
    }
    
    /**
     * Extract value from JSON string
     */
    private String extractJsonValue(String json, String key, String defaultValue) {
        try {
            String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]+)\"";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(json);
            if (m.find()) {
                return m.group(1);
            }
            
            pattern = "\"" + key + "\"\\s*:\\s*([^,}\\s]+)";
            p = java.util.regex.Pattern.compile(pattern);
            m = p.matcher(json);
            if (m.find()) {
                return m.group(1);
            }
        } catch (Exception e) {
            log.warn("Error extracting {} from JSON", key, e);
        }
        return defaultValue;
    }
}

