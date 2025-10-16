package com.health.multiagent.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO para requisição de submissão de sintomas
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SymptomsRequest {
    
    @NotBlank(message = "Patient ID is required")
    private String patientId;
    
    @NotBlank(message = "Symptoms description is required")
    private String symptoms;
    
    private String medicalHistory;
    
    @Builder.Default
    private List<String> currentMedications = new ArrayList<>();
}

