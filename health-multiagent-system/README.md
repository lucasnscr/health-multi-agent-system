# Health Multi-Agent System

Sistema multi-agente para área de saúde implementado com **LangGraph4j**, **Spring AI** e **Qwen 3 8B**, seguindo o padrão de **Agent Handoff** com aprovação humana (HITL - Human-in-the-Loop).

## Visão Geral

Este sistema demonstra uma arquitetura moderna de agentes de IA especializados que trabalham em conjunto para processar informações de saúde de pacientes, fornecendo avaliações completas através de um fluxo sequencial e coordenado.

### Fluxo de Agentes

O sistema executa um fluxo sequencial de 4 agentes especializados:

1. **Triage Agent** - Avalia sintomas e determina nível de risco (LOW, MEDIUM, HIGH, CRITICAL)
2. **Pharmacist Agent** - Analisa medicações atuais e identifica interações medicamentosas
3. **Exam Agent** - Recomenda exames diagnósticos baseados em sintomas e análises anteriores
4. **EMR/Comms Agent** - Gera documentação FHIR e comunicações (requer aprovação humana)

### Arquitetura

O sistema utiliza o padrão **Agent Handoff** onde cada agente é implementado como uma ferramenta (Tool) que encapsula lógica especializada. O contexto é transferido sequencialmente entre os agentes, permitindo que cada um contribua com sua expertise específica.

A aprovação humana (HITL) é implementada através de **InterruptNode**, pausando a execução antes de finalizar a documentação FHIR, garantindo supervisão médica adequada.

## Tecnologias

- **Java 21** - Linguagem de programação
- **Spring Boot 3.5.0** - Framework de aplicação
- **Spring AI 1.0.0-M6** - Integração com modelos de IA
- **LangGraph4j** - Orquestração de agentes
- **Qwen 3 8B** - Modelo de linguagem (via Ollama)
- **Maven** - Gerenciamento de dependências
- **Lombok** - Redução de boilerplate
- **SpringDoc OpenAPI** - Documentação da API

## Pré-requisitos

### 1. Java 21

```bash
# Instalar via SDKMAN
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 21.0.2-tem
```

### 2. Maven

```bash
# Instalar via SDKMAN
sdk install maven
```

### 3. Ollama com Qwen 3

```bash
# Instalar Ollama
curl -fsSL https://ollama.com/install.sh | sh

# Baixar modelo Qwen 2.5 3B
ollama pull qwen2.5:3b

# Ou usar o modelo 8B (requer mais memória)
ollama pull qwen2.5:8b

# Iniciar servidor Ollama
ollama serve
```

## Instalação e Execução

### 1. Clonar o projeto

```bash
git clone <repository-url>
cd health-multiagent-system
```

### 2. Compilar o projeto

```bash
mvn clean install
```

### 3. Executar a aplicação

```bash
mvn spring-boot:run
```

A aplicação estará disponível em: `http://localhost:8080/api`

### 4. Acessar documentação Swagger

Abra o navegador em: `http://localhost:8080/api/swagger-ui.html`

## Uso da API

### Fluxo Normal (Aprovação Direta)

#### 1. Submeter Sintomas do Paciente

**Endpoint:** `POST /api/health-assessment/symptoms`

**Request Body:**
```json
{
  "patientId": "PAT-12345",
  "symptoms": "Febre alta há 3 dias, dor de cabeça intensa, dor no corpo, náusea",
  "medicalHistory": "Hipertensão controlada, diabetes tipo 2",
  "currentMedications": [
    "Losartana 50mg",
    "Metformina 850mg"
  ]
}
```

**Response:**
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "AWAITING_APPROVAL",
  "currentAgent": "EMR_COMMS",
  "message": "Assessment completed. Awaiting human approval for FHIR documentation.",
  "interruptionInfo": {
    "nodeId": "emr_comms_approval",
    "label": "Approve FHIR documentation and communications?",
    "type": "fhir_approval"
  },
  "data": {
    "patientId": "PAT-12345",
    "riskLevel": "MEDIUM",
    "symptomsSummary": "Patient presents with fever, headache, body aches, and nausea...",
    "drugInteractions": [],
    "recommendedLabExams": ["Complete Blood Count", "C-Reactive Protein"],
    "fhirDocument": "{...}",
    "communicationText": "..."
  }
}
```

### 2. Verificar Status da Avaliação

**Endpoint:** `GET /api/health-assessment/status/{sessionId}`

**Response:**
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "AWAITING_APPROVAL",
  "currentAgent": "EMR_COMMS",
  "message": "Assessment completed. Awaiting human approval.",
  "data": { ... }
}
```

### 3. Aprovar Documentação

**Endpoint:** `POST /api/health-assessment/approve/{sessionId}`

**Request Body:**
```json
{
  "decision": "APPROVED",
  "comments": "Documentação revisada e aprovada para envio"
}
```

**Response:**
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "COMPLETED",
  "message": "Assessment completed and approved."
}
```

### Fluxo de Reprocessamento (Rejeição com Feedback)

Quando o médico não aprova o diagnóstico, ele pode **rejeitar a avaliação e fornecer feedback específico**. O sistema então **reprocessa toda a avaliação** incorporando esse feedback.

#### 1. Rejeitar com Feedback

**Endpoint:** `POST /api/health-assessment/approve/{sessionId}`

**Request Body:**
```json
{
  "decision": "REJECTED",
  "comments": "A avaliação não considerou adequadamente o histórico de colecistite. Recomendo incluir ultrassom abdominal como exame prioritário e reavaliar o nível de risco."
}
```

**Response (após reprocessamento automático):**
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "AWAITING_APPROVAL",
  "message": "Reprocessed assessment ready (iteration 1). Please review and approve or provide additional feedback.",
  "data": {
    "riskLevel": "HIGH",
    "recommendedImagingExams": ["Ultrassom Abdominal"],
    "reprocessingCount": 1,
    "physicianFeedback": "A avaliação não considerou adequadamente...",
    "assessmentHistory": [
      "Iteration 0 - Risk: MEDIUM, Exams: 2, Physician Feedback: ..."
    ]
  }
}
```

#### 2. Revisar e Aprovar/Rejeitar Novamente

O médico pode:
- **Aprovar** a avaliação reprocessada
- **Rejeitar novamente** com novo feedback (até 3 iterações no total)

**Características do Reprocessamento:**
- ✅ Feedback do médico é incorporado em **todos os 4 agentes**
- ✅ Histórico de avaliações anteriores é preservado
- ✅ Máximo de **3 iterações** de reprocessamento
- ✅ Reprocessamento é **automático** após rejeição

**Veja documentação completa:** [REPROCESSING.md](REPROCESSING.md)

## Estrutura do Projeto

```
health-multiagent-system/
├── src/
│   ├── main/
│   │   ├── java/com/health/multiagent/
│   │   │   ├── agent/              # Implementação dos agentes
│   │   │   │   ├── TriageAgent.java
│   │   │   │   ├── PharmacistAgent.java
│   │   │   │   ├── ExamAgent.java
│   │   │   │   └── EMRCommsAgent.java
│   │   │   ├── config/             # Configurações
│   │   │   │   └── OpenAPIConfig.java
│   │   │   ├── controller/         # Controllers REST
│   │   │   │   └── HealthAssessmentController.java
│   │   │   ├── model/              # Modelos de dados
│   │   │   │   ├── PatientAssessmentState.java
│   │   │   │   ├── RiskAssessment.java
│   │   │   │   ├── PharmacyAnalysis.java
│   │   │   │   ├── ExamRecommendations.java
│   │   │   │   ├── FHIRDocumentation.java
│   │   │   │   ├── SymptomsRequest.java
│   │   │   │   ├── AssessmentResponse.java
│   │   │   │   └── ApprovalRequest.java
│   │   │   ├── service/            # Serviços
│   │   │   │   └── HealthAssessmentService.java
│   │   │   ├── exception/          # Tratamento de exceções
│   │   │   │   └── GlobalExceptionHandler.java
│   │   │   └── HealthMultiAgentApplication.java
│   │   └── resources/
│   │       └── application.yml
│   └── test/
│       └── java/com/health/multiagent/
│           └── HealthAssessmentControllerTest.java
├── pom.xml
└── README.md
```

## Configuração

### application.yml

As principais configurações podem ser ajustadas em `src/main/resources/application.yml`:

```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        options:
          model: qwen2.5:3b  # Ou qwen2.5:8b
          temperature: 0.1
          
health:
  multiagent:
    session:
      timeout-minutes: 30
    agents:
      triage:
        enabled: true
        system-prompt: "..."
      # ... outros agentes
```

## Padrões Implementados

### Agent Handoff Pattern

Cada agente é implementado estendendo `AbstractAgentExecutor` e definindo suas ferramentas internas. O contexto é transferido automaticamente entre agentes através do `PatientAssessmentState`.

### Human-in-the-Loop (HITL)

Implementado usando `InterruptNode` no último agente (EMR/Comms), pausando a execução antes de finalizar a documentação FHIR e aguardando aprovação humana via API.

### Estado Compartilhado

O `PatientAssessmentState` mantém todo o contexto da avaliação, sendo enriquecido por cada agente no fluxo:

- **Triage** → adiciona riskLevel, symptomsSummary, recommendations
- **Pharmacist** → adiciona drugInteractions, contraindications
- **Exam** → adiciona recommendedExams, priority
- **EMR/Comms** → adiciona fhirDocument, communicationText

### Reprocessamento com Feedback Médico

Quando o médico rejeita uma avaliação, o sistema:

1. **Salva o histórico** da avaliação atual
2. **Armazena o feedback** do médico
3. **Limpa resultados anteriores** (mantendo dados do paciente)
4. **Reexecuta todos os 4 agentes** incorporando o feedback nos prompts
5. **Aguarda nova aprovação** (até 3 iterações)

O feedback é injetado nos prompts de todos os agentes, permitindo que cada um ajuste sua análise baseado nas preocupações específicas do médico.

**Documentação completa:** [REPROCESSING.md](REPROCESSING.md)

## Testes

### Executar testes

```bash
mvn test
```

**Nota:** Os testes de integração requerem que o Ollama esteja rodando com o modelo qwen2.5:3b disponível.

### Teste de Reprocessamento

Execute o script de teste do fluxo de reprocessamento:

```bash
./test-reprocessing.sh
```

Este script demonstra:
1. Submissão inicial de sintomas
2. Rejeição com feedback específico do médico
3. Reprocessamento automático incorporando feedback
4. Aprovação da avaliação reprocessada
5. Exibição do histórico completo

### Teste manual com cURL

**Fluxo normal:**
```bash
# 1. Submeter sintomas
curl -X POST http://localhost:8080/api/health-assessment/symptoms \
  -H "Content-Type: application/json" \
  -d '{
    "patientId": "TEST-001",
    "symptoms": "Febre alta, dor de cabeça, tosse seca",
    "medicalHistory": "Nenhum",
    "currentMedications": []
  }'

# 2. Verificar status (usar sessionId retornado)
curl http://localhost:8080/api/health-assessment/status/{sessionId}

# 3. Aprovar documentação
curl -X POST http://localhost:8080/api/health-assessment/approve/{sessionId} \
  -H "Content-Type: application/json" \
  -d '{
    "decision": "APPROVED",
    "comments": "Aprovado"
  }'
```

**Fluxo com reprocessamento:**
```bash
# 1. Submeter sintomas
curl -X POST http://localhost:8080/api/health-assessment/symptoms \
  -H "Content-Type: application/json" \
  -d '{...}'

# 2. Rejeitar com feedback
curl -X POST http://localhost:8080/api/health-assessment/approve/{sessionId} \
  -H "Content-Type: application/json" \
  -d '{
    "decision": "REJECTED",
    "comments": "Por favor, reavalie o nível de risco considerando o histórico do paciente."
  }'

# 3. Verificar reprocessamento (automático)
curl http://localhost:8080/api/health-assessment/status/{sessionId}
# Resposta incluirá: reprocessingCount: 1, physicianFeedback, assessmentHistory

# 4. Aprovar avaliação reprocessada
curl -X POST http://localhost:8080/api/health-assessment/approve/{sessionId} \
  -H "Content-Type: application/json" \
  -d '{
    "decision": "APPROVED",
    "comments": "Avaliação reprocessada adequadamente"
  }'
```

## Considerações de Produção

### Segurança

- Implementar autenticação e autorização (OAuth2, JWT)
- Criptografar dados sensíveis de saúde
- Implementar rate limiting
- Validar e sanitizar todas as entradas

### Persistência

- Substituir `ConcurrentHashMap` por banco de dados relacional (PostgreSQL)
- Implementar cache distribuído (Redis)
- Adicionar auditoria de todas as operações

### Escalabilidade

- Implementar processamento assíncrono com mensageria (RabbitMQ, Kafka)
- Adicionar balanceamento de carga
- Implementar circuit breaker (Resilience4j)
- Monitoramento com Prometheus e Grafana

### Conformidade

- Garantir conformidade com HIPAA (EUA) ou LGPD (Brasil)
- Implementar logs auditáveis
- Backup e recuperação de desastres
- Políticas de retenção de dados

## Referências

- [LangGraph4j - Agent Handoff](https://bsorrentino.github.io/bsorrentino/ai/2025/05/10/Langgraph4j-agent-handoff.html)
- [LangGraph4j - Human-in-the-Loop](https://bsorrentino.github.io/bsorrentino/ai/2025/07/13/LangGraph4j-Agent-with-approval.html)
- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- [FHIR Standard](https://www.hl7.org/fhir/)

## Licença

Apache License 2.0

## Contribuindo

Contribuições são bem-vindas! Por favor, abra uma issue ou pull request.

## Contato

Para dúvidas ou sugestões, entre em contato através de: contact@healthmultiagent.com

