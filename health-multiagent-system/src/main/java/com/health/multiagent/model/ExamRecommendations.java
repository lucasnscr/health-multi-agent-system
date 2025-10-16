package com.health.multiagent.model;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.List;

/**
 * Resultado das recomendações de exames do Exam Agent
 */
public record ExamRecommendations(
    @JsonPropertyDescription("List of recommended laboratory exams")
    List<String> laboratoryExams,
    
    @JsonPropertyDescription("List of recommended imaging exams")
    List<String> imagingExams,
    
    @JsonPropertyDescription("Priority level: ROUTINE, URGENT, EMERGENCY")
    String priority,
    
    @JsonPropertyDescription("Rationale for exam recommendations")
    String rationale
) {}

