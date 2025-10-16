package com.health.multiagent.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração do OpenAPI (Swagger) para documentação da API
 */
@Configuration
public class OpenAPIConfig {
    
    @Bean
    public OpenAPI healthMultiAgentOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Health Multi-Agent System API")
                .description("""
                    Sistema multi-agente para área de saúde implementado com LangGraph4j e Spring AI.
                    
                    ## Fluxo de Agentes
                    
                    O sistema executa um fluxo sequencial de 4 agentes especializados:
                    
                    1. **Triage Agent** - Avalia sintomas e determina nível de risco
                    2. **Pharmacist Agent** - Analisa medicações e identifica interações
                    3. **Exam Agent** - Recomenda exames diagnósticos
                    4. **EMR/Comms Agent** - Gera documentação FHIR (requer aprovação humana)
                    
                    ## Uso da API
                    
                    1. Submeta sintomas do paciente via `POST /symptoms`
                    2. Receba um `sessionId` e aguarde o status `AWAITING_APPROVAL`
                    3. Aprove ou rejeite via `POST /approve/{sessionId}`
                    4. Consulte o status a qualquer momento via `GET /status/{sessionId}`
                    """)
                .version("1.0.0")
                .contact(new Contact()
                    .name("Health Multi-Agent Team")
                    .email("contact@healthmultiagent.com"))
                .license(new License()
                    .name("Apache 2.0")
                    .url("https://www.apache.org/licenses/LICENSE-2.0.html")));
    }
}

