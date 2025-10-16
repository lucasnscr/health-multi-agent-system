package com.health.multiagent.agent;

import com.health.multiagent.model.ExamRecommendations;
import com.health.multiagent.model.PatientAssessmentState;
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
 * Exam Agent - Terceiro agente do fluxo
 * 
 * Responsabilidades:
 * - Recomendar exames laboratoriais
 * - Recomendar exames de imagem
 * - Priorizar exames por urgência
 * - Justificar recomendações
 */
@Slf4j
@Component
public class ExamAgent {
    
    private final ChatModel chatModel;
    
    private static final String EXAM_PROMPT_TEMPLATE = """
        You are a diagnostic exam recommendation agent.
        
        Patient Information:
        - Patient ID: {patientId}
        - Symptoms: {symptoms}
        - Medical History: {medicalHistory}
        - Risk Level: {riskLevel}
        - Triage Recommendations: {triageRecommendations}
        - Drug Interactions: {drugInteractions}
        - Pharmacy Recommendations: {pharmacyRecommendations}
        
        {physicianFeedbackSection}
        
        Based on the complete patient assessment, recommend appropriate diagnostic exams.
        
        Provide:
        1. List of recommended laboratory exams
        2. List of recommended imaging exams
        3. Priority level (ROUTINE, URGENT, or EMERGENCY)
        4. Rationale explaining why these exams are recommended
        
        Respond in JSON format with the following structure:
        {{
          "laboratoryExams": ["exam1", "exam2"],
          "imagingExams": ["exam1", "exam2"],
          "priority": "ROUTINE|URGENT|EMERGENCY",
          "rationale": "detailed explanation"
        }}
        
        Consider:
        - Symptoms and their severity
        - Risk level from triage
        - Medication interactions that may require monitoring
        - Differential diagnosis possibilities
        - Cost-effectiveness and diagnostic value
        
        Prioritize exams that will provide the most diagnostic value.
        If symptoms are mild, recommend only essential exams.
        All content give in portuguese.
        """;
    
    public ExamAgent(ChatModel chatModel) {
        this.chatModel = chatModel;
    }
    
    /**
     * Executa recomendação de exames diagnósticos
     */
    public ExamRecommendations recommendExams(PatientAssessmentState state) {
        log.info("Starting exam recommendations for patient: {}", state.getPatientId());
        
        try {
            PromptTemplate promptTemplate = new PromptTemplate(EXAM_PROMPT_TEMPLATE);
            
            // Adicionar seção de feedback do médico se houver reprocessamento
            String feedbackSection = "";
            if (state.getPhysicianFeedback() != null && !state.getPhysicianFeedback().isEmpty()) {
                feedbackSection = String.format("""
                    
                    IMPORTANT - Physician Feedback from Previous Assessment:
                    %s
                    
                    Please incorporate this feedback in your exam recommendations.
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
                "riskLevel", state.getRiskLevel() != null ? state.getRiskLevel() : "UNKNOWN",
                "triageRecommendations", state.getTriageRecommendations() != null 
                    ? state.getTriageRecommendations() 
                    : "None",
                "drugInteractions", state.getDrugInteractions() != null && !state.getDrugInteractions().isEmpty()
                    ? String.join(", ", state.getDrugInteractions())
                    : "None identified",
                "pharmacyRecommendations", state.getPharmacistRecommendations() != null
                    ? state.getPharmacistRecommendations()
                    : "None",
                "physicianFeedbackSection", feedbackSection
            );
            
            Prompt prompt = promptTemplate.create(variables);
            String response = chatModel.call(prompt).getResult().getOutput().getText();
            
            log.debug("Exam agent response: {}", response);

            if (response == null || response.isEmpty()) {
                throw new RuntimeException("Empty response from exam agent");
            }
            ExamRecommendations recommendations = parseExamRecommendations(response);
            
            log.info("Exam recommendations completed - Priority: {}, Lab exams: {}, Imaging: {}", 
                recommendations.priority(), 
                recommendations.laboratoryExams().size(),
                recommendations.imagingExams().size());
            
            return recommendations;
            
        } catch (Exception e) {
            log.error("Error during exam recommendations", e);
            return new ExamRecommendations(
                List.of(),
                List.of(),
                "URGENT",
                "Error during exam recommendation: " + e.getMessage() + ". Manual review required."
            );
        }
    }
    
    /**
     * Parse JSON response to ExamRecommendations object
     */
    private ExamRecommendations parseExamRecommendations(String jsonResponse) {
        String cleaned = jsonResponse.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
        
        List<String> labExams = extractJsonArray(cleaned, "laboratoryExams");
        List<String> imagingExams = extractJsonArray(cleaned, "imagingExams");
        String priority = extractJsonValue(cleaned, "priority", "ROUTINE");
        String rationale = extractJsonValue(cleaned, "rationale", "Standard diagnostic workup");
        
        return new ExamRecommendations(labExams, imagingExams, priority, rationale);
    }
    
    /**
     * Extract array from JSON string
     */
    private List<String> extractJsonArray(String json, String key) {
        try {
            String pattern = "\"" + key + "\"\\s*:\\s*\\[([^\\]]+)\\]";
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
        } catch (Exception e) {
            log.warn("Error extracting {} from JSON", key, e);
        }
        return defaultValue;
    }
}

