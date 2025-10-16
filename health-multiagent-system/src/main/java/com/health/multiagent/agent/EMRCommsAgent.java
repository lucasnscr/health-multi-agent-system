package com.health.multiagent.agent;

import com.health.multiagent.model.FHIRDocumentation;
import com.health.multiagent.model.PatientAssessmentState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * EMR/Communications Agent - Quarto e último agente do fluxo
 * Responsabilidades:
 * - Gerar documentação FHIR completa
 * - Preparar comunicações para profissionais de saúde
 * - Consolidar todas as informações dos agentes anteriores
 * - Requer aprovação humana (HITL) antes de finalizar
 */
@Slf4j
@Component
public class EMRCommsAgent {
    
    private final ChatModel chatModel;
    
    private static final String EMR_PROMPT_TEMPLATE = """
        You are responsible for generating accurate FHIR documentation and healthcare communications.
        
        Complete Patient Assessment:
        - Patient ID: {patientId}
        - Symptoms: {symptoms}
        - Medical History: {medicalHistory}
        - Current Medications: {currentMedications}
        
        Triage Assessment:
        - Risk Level: {riskLevel}
        - Summary: {symptomsSummary}
        - Recommendations: {triageRecommendations}
        
        Pharmacy Analysis:
        - Drug Interactions: {drugInteractions}
        - Contraindications: {contraindications}
        - Recommendations: {pharmacyRecommendations}
        
        Exam Recommendations:
        - Laboratory Exams: {labExams}
        - Imaging Exams: {imagingExams}
        - Priority: {examPriority}
        - Rationale: {examRationale}
        
        {reprocessingInfoSection}
        {physicianFeedbackSection}
        
        Generate:
        1. A FHIR-formatted document (simplified JSON structure)
        2. A clear communication text for healthcare providers
        3. Document type (ASSESSMENT, REFERRAL, or PRESCRIPTION)
        
        Respond in JSON format with the following structure:
        {{
          "fhirDocument": "{{FHIR JSON structure}}",
          "communicationText": "clear text for healthcare providers",
          "documentType": "ASSESSMENT|REFERRAL|PRESCRIPTION"
        }}
        
        The FHIR document should include:
        - Patient information
        - Clinical impression (from triage)
        - Medication analysis
        - Diagnostic plan (exams)
        - Recommendations
        
        The communication text should be:
        - Professional and concise
        - Highlight critical findings
        - Include clear action items
        - Easy for healthcare providers to understand
        
        Ensure all information is accurate and complete in portuguese.
        """;
    
    public EMRCommsAgent(ChatModel chatModel) {
        this.chatModel = chatModel;
    }
    
    /**
     * Gera documentação FHIR e comunicações
     */
    public FHIRDocumentation generateDocumentation(PatientAssessmentState state) {
        log.info("Starting FHIR documentation generation for patient: {}", state.getPatientId());
        
        try {
            PromptTemplate promptTemplate = new PromptTemplate(EMR_PROMPT_TEMPLATE);
            
            // Adicionar informações de reprocessamento se aplicável
            String reprocessingInfo = "";
            String feedbackSection = "";
            
            if (state.getReprocessingCount() > 0) {
                reprocessingInfo = String.format("""
                    
                    Reprocessing Information:
                    - Current iteration: %d of %d
                    - Assessment history: %s
                    """,
                    state.getReprocessingCount(),
                    state.getMaxReprocessingIterations(),
                    state.getAssessmentHistory() != null && !state.getAssessmentHistory().isEmpty()
                        ? String.join("; ", state.getAssessmentHistory())
                        : "None"
                );
                
                if (state.getPhysicianFeedback() != null && !state.getPhysicianFeedback().isEmpty()) {
                    feedbackSection = String.format("""
                        
                        CRITICAL - Physician Feedback that MUST be addressed:
                        %s
                        
                        Ensure the new documentation addresses all physician concerns.
                        """,
                        state.getPhysicianFeedback()
                    );
                }
            }

            Map<String, Object> variables = new java.util.HashMap<>();
            variables.put("patientId", state.getPatientId() != null ? state.getPatientId() : "UNKNOWN");
            variables.put("symptoms", state.getSymptoms() != null ? state.getSymptoms() : "No symptoms");
            variables.put("medicalHistory", state.getMedicalHistory() != null ? state.getMedicalHistory() : "No history");
            variables.put("currentMedications", state.getCurrentMedications() != null && !state.getCurrentMedications().isEmpty()
                ? String.join(", ", state.getCurrentMedications())
                : "None");
            variables.put("riskLevel", state.getRiskLevel() != null ? state.getRiskLevel() : "UNKNOWN");
            variables.put("symptomsSummary", state.getSymptomsSummary() != null ? state.getSymptomsSummary() : "N/A");
            variables.put("triageRecommendations", state.getTriageRecommendations() != null
                ? state.getTriageRecommendations() : "None");
            variables.put("drugInteractions", state.getDrugInteractions() != null && !state.getDrugInteractions().isEmpty()
                ? String.join(", ", state.getDrugInteractions()) : "None");
            variables.put("contraindications", state.getContraindications() != null && !state.getContraindications().isEmpty()
                ? String.join(", ", state.getContraindications()) : "None");
            variables.put("pharmacyRecommendations", state.getPharmacistRecommendations() != null
                ? state.getPharmacistRecommendations() : "None");
            variables.put("labExams", state.getRecommendedLabExams() != null && !state.getRecommendedLabExams().isEmpty()
                ? String.join(", ", state.getRecommendedLabExams()) : "None");
            variables.put("imagingExams", state.getRecommendedImagingExams() != null && !state.getRecommendedImagingExams().isEmpty()
                ? String.join(", ", state.getRecommendedImagingExams()) : "None");
            variables.put("examPriority", state.getExamPriority() != null ? state.getExamPriority() : "ROUTINE");
            variables.put("examRationale", state.getExamRecommendations() != null ? state.getExamRecommendations() : "N/A");
            variables.put("reprocessingInfoSection", reprocessingInfo);
            variables.put("physicianFeedbackSection", feedbackSection);
            
            Prompt prompt = promptTemplate.create(variables);
            String response = chatModel.call(prompt).getResult().getOutput().getText();
            
            log.debug("EMR/Comms agent response: {}", response);

            if (response == null || response.isEmpty()) {
                throw new RuntimeException("Empty response from exam agent");
            }
            FHIRDocumentation documentation = parseFHIRDocumentation(response);
            
            log.info("FHIR documentation generated - Type: {}", documentation.documentType());
            
            return documentation;
            
        } catch (Exception e) {
            log.error("Error during FHIR documentation generation", e);
            return new FHIRDocumentation(
                "{\"error\": \"Failed to generate FHIR document: " + e.getMessage() + "\"}",
                "ERROR: Documentation generation failed. Manual review required.",
                "ASSESSMENT"
            );
        }
    }
    
    /**
     * Parse JSON response to FHIRDocumentation object
     */
    private FHIRDocumentation parseFHIRDocumentation(String jsonResponse) {
        String cleaned = jsonResponse.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
        
        // Extract FHIR document (may be nested JSON)
        String fhirDoc = extractNestedJson(cleaned, "fhirDocument");
        if (fhirDoc == null || fhirDoc.isEmpty()) {
            fhirDoc = "{\"resourceType\": \"Bundle\", \"type\": \"document\"}";
        }
        
        String commText = extractJsonValue(cleaned, "communicationText", "Documentation generated");
        String docType = extractJsonValue(cleaned, "documentType", "ASSESSMENT");
        
        return new FHIRDocumentation(fhirDoc, commText, docType);
    }
    
    /**
     * Extract nested JSON object
     */
    private String extractNestedJson(String json, String key) {
        try {
            // Find the key and extract the nested JSON
            int keyIndex = json.indexOf("\"" + key + "\"");
            if (keyIndex == -1) return null;
            
            int startIndex = json.indexOf("{", keyIndex);
            if (startIndex == -1) {
                // Try to extract as string value
                return extractJsonValue(json, key, "{}");
            }
            
            int braceCount = 1;
            int endIndex = startIndex + 1;
            
            while (endIndex < json.length() && braceCount > 0) {
                char c = json.charAt(endIndex);
                if (c == '{') braceCount++;
                else if (c == '}') braceCount--;
                endIndex++;
            }
            
            if (braceCount == 0) {
                return json.substring(startIndex, endIndex);
            }
        } catch (Exception e) {
            log.warn("Error extracting nested JSON for {}", key, e);
        }
        return "{}";
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

