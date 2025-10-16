package com.health.multiagent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.health.multiagent.model.ApprovalRequest;
import com.health.multiagent.model.SymptomsRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração para o Health Assessment Controller
 * 
 * NOTA: Estes testes requerem que o Ollama esteja rodando localmente
 * com o modelo qwen2.5:3b disponível.
 */
@SpringBootTest
@AutoConfigureMockMvc
class HealthAssessmentControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    void testSubmitSymptoms_Success() throws Exception {
        SymptomsRequest request = SymptomsRequest.builder()
            .patientId("TEST-001")
            .symptoms("Febre alta há 3 dias, dor de cabeça intensa, dor no corpo")
            .medicalHistory("Hipertensão controlada")
            .currentMedications(List.of("Losartana 50mg"))
            .build();
        
        mockMvc.perform(post("/api/health-assessment/symptoms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionId").exists())
            .andExpect(jsonPath("$.status").value("AWAITING_APPROVAL"));
    }
    
    @Test
    void testSubmitSymptoms_ValidationError() throws Exception {
        SymptomsRequest request = SymptomsRequest.builder()
            .patientId("") // Invalid: empty
            .symptoms("Febre")
            .build();
        
        mockMvc.perform(post("/api/health-assessment/symptoms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value("ERROR"));
    }
    
    @Test
    void testGetStatus_NotFound() throws Exception {
        mockMvc.perform(get("/api/health-assessment/status/invalid-session-id"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value("ERROR"));
    }
    
    @Test
    void testApproveAction_ValidationError() throws Exception {
        ApprovalRequest request = ApprovalRequest.builder()
            .decision("INVALID") // Invalid decision
            .build();
        
        mockMvc.perform(post("/api/health-assessment/approve/some-session-id")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
}

