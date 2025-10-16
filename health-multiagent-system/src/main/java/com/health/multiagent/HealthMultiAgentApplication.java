package com.health.multiagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Health Multi-Agent System Application
 * 
 * Sistema multi-agente para área de saúde implementado com LangGraph4j e Spring AI.
 * 
 * Fluxo de agentes:
 * 1. Triage Agent - Avalia sintomas e determina nível de risco
 * 2. Pharmacist Agent - Analisa medicações e interações
 * 3. Exam Agent - Recomenda exames diagnósticos
 * 4. EMR/Comms Agent - Gera documentação FHIR com aprovação humana (HITL)
 * 
 * @author Health Multi-Agent Team
 * @version 1.0.0
 */
@SpringBootApplication
public class HealthMultiAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(HealthMultiAgentApplication.class, args);
    }

}

