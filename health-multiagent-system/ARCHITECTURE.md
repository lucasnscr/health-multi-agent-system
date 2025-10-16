# Arquitetura Técnica - Health Multi-Agent System

## Visão Geral da Arquitetura

O **Health Multi-Agent System** implementa uma arquitetura de microserviços baseada em agentes de IA especializados, utilizando o padrão **Agent Handoff** para coordenação e **Human-in-the-Loop (HITL)** para supervisão humana.

## Diagrama de Arquitetura

```
┌─────────────────────────────────────────────────────────────────┐
│                         Cliente (REST API)                       │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                  HealthAssessmentController                      │
│                    (Spring REST Controller)                      │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                  HealthAssessmentService                         │
│                   (Orquestrador de Agentes)                      │
└───────────────────────────┬─────────────────────────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
        ▼                   ▼                   ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│ TriageAgent  │ -> │ Pharmacist   │ -> │  ExamAgent   │
│              │    │    Agent     │    │              │
└──────────────┘    └──────────────┘    └──────────────┘
                                                │
                                                ▼
                                        ┌──────────────┐
                                        │ EMR/Comms    │
                                        │   Agent      │
                                        └──────┬───────┘
                                               │
                                               ▼
                                        [InterruptNode]
                                        (HITL Approval)
                                               │
                                               ▼
                                        ┌──────────────┐
                                        │   FHIR Doc   │
                                        │ + Comms Text │
                                        └──────────────┘
```

## Componentes Principais

### 1. Camada de Apresentação

#### HealthAssessmentController
**Responsabilidade:** Expor endpoints REST para interação com o sistema.

**Endpoints:**
- `POST /symptoms` - Iniciar avaliação de paciente
- `GET /status/{sessionId}` - Consultar status da avaliação
- `POST /approve/{sessionId}` - Aprovar/rejeitar documentação

**Tecnologias:**
- Spring Web MVC
- Jakarta Validation
- SpringDoc OpenAPI

### 2. Camada de Serviço

#### HealthAssessmentService
**Responsabilidade:** Orquestrar o fluxo sequencial de agentes.

**Funções principais:**
- Gerenciar sessões de avaliação
- Coordenar execução sequencial dos agentes
- Transferir contexto entre agentes
- Processar aprovações humanas

**Padrões implementados:**
- Service Layer Pattern
- State Management Pattern
- Session Management

### 3. Camada de Agentes

Cada agente é implementado como um componente Spring especializado que utiliza o ChatModel do Spring AI para processar informações.

#### TriageAgent
**Responsabilidade:** Avaliação inicial de risco.

**Entrada:**
- Sintomas do paciente
- Histórico médico
- Medicações atuais

**Saída:**
- Nível de risco (LOW, MEDIUM, HIGH, CRITICAL)
- Resumo dos sintomas
- Recomendações iniciais
- Flag de urgência

**Lógica:**
- Análise de gravidade dos sintomas
- Identificação de sinais de alerta
- Priorização baseada em risco

#### PharmacistAgent
**Responsabilidade:** Análise farmacêutica.

**Entrada:**
- Contexto da triagem
- Medicações atuais
- Sintomas

**Saída:**
- Interações medicamentosas identificadas
- Contraindicações
- Recomendações de ajuste
- Flag de preocupações de segurança

**Lógica:**
- Verificação de interações drug-drug
- Análise de contraindicações drug-disease
- Avaliação de dosagens

#### ExamAgent
**Responsabilidade:** Recomendação de exames diagnósticos.

**Entrada:**
- Contexto completo (triagem + farmácia)
- Sintomas
- Nível de risco

**Saída:**
- Exames laboratoriais recomendados
- Exames de imagem recomendados
- Prioridade (ROUTINE, URGENT, EMERGENCY)
- Justificativa das recomendações

**Lógica:**
- Diagnóstico diferencial
- Custo-benefício dos exames
- Priorização por valor diagnóstico

#### EMRCommsAgent
**Responsabilidade:** Geração de documentação e comunicações.

**Entrada:**
- Contexto completo de todos os agentes anteriores

**Saída:**
- Documento FHIR estruturado
- Texto de comunicação para profissionais
- Tipo de documento (ASSESSMENT, REFERRAL, PRESCRIPTION)

**Lógica:**
- Consolidação de informações
- Formatação FHIR
- Geração de comunicação clara

**HITL:** Este agente requer aprovação humana antes de finalizar.

### 4. Camada de Modelo

#### PatientAssessmentState
**Responsabilidade:** Manter estado compartilhado entre agentes.

**Características:**
- Imutabilidade parcial (builder pattern)
- Serialização JSON
- Validação de dados

**Campos principais:**
- Informações do paciente
- Resultados de cada agente
- Metadados de sessão
- Status de aprovação

#### Records de Resultado
**Tipos:**
- `RiskAssessment` - Resultado da triagem
- `PharmacyAnalysis` - Resultado da análise farmacêutica
- `ExamRecommendations` - Recomendações de exames
- `FHIRDocumentation` - Documentação final

**Características:**
- Imutáveis (Java Records)
- Type-safe
- Auto-documentados com JsonPropertyDescription

## Padrões de Design

### 1. Agent Handoff Pattern

**Conceito:** Cada agente é tratado como uma "ferramenta" que pode ser invocada pelo orquestrador, transferindo contexto entre eles.

**Implementação:**
```java
// Cada agente processa o estado e retorna resultado
RiskAssessment result = triageAgent.assessPatient(state);

// Estado é atualizado com resultado
state.setRiskLevel(result.riskLevel());

// Próximo agente recebe estado atualizado
PharmacyAnalysis analysis = pharmacistAgent.analyzeMedications(state);
```

**Benefícios:**
- Separação de responsabilidades
- Reutilização de agentes
- Fácil extensão com novos agentes
- Testabilidade individual

### 2. Human-in-the-Loop (HITL) Pattern

**Conceito:** Pausar execução automática para aguardar decisão humana em pontos críticos.

**Implementação:**
```java
// Agente gera documentação
FHIRDocumentation doc = emrCommsAgent.generateDocumentation(state);

// Estado muda para aguardar aprovação
state.setStatus("AWAITING_APPROVAL");
state.setApprovalStatus("PENDING");

// Sistema aguarda chamada de aprovação
// POST /approve/{sessionId}

// Após aprovação, sistema continua ou finaliza
if (approved) {
    state.setStatus("COMPLETED");
}
```

**Benefícios:**
- Supervisão médica garantida
- Responsabilidade humana mantida
- Auditoria de decisões
- Segurança do paciente

### 3. State Management Pattern

**Conceito:** Centralizar estado da aplicação em um objeto compartilhado.

**Implementação:**
- `PatientAssessmentState` como single source of truth
- Estado enriquecido progressivamente
- Armazenamento em memória (ConcurrentHashMap)
- Identificação por sessionId (UUID)

**Evolução para produção:**
- Persistência em banco de dados relacional
- Cache distribuído (Redis)
- Event sourcing para auditoria

### 4. Prompt Engineering Pattern

**Conceito:** Templates de prompts estruturados para cada agente.

**Implementação:**
```java
private static final String TRIAGE_PROMPT_TEMPLATE = """
    You are a medical triage agent. Analyze the patient information...
    
    Patient Information:
    - Patient ID: {patientId}
    - Symptoms: {symptoms}
    ...
    
    Respond in JSON format with the following structure:
    {
      "riskLevel": "LOW|MEDIUM|HIGH|CRITICAL",
      ...
    }
    """;
```

**Benefícios:**
- Respostas estruturadas e previsíveis
- Fácil manutenção e ajuste
- Documentação embutida
- Validação de saída

## Fluxo de Dados

### Fluxo de Execução Completo

```
1. Cliente envia POST /symptoms
   └─> SymptomsRequest (DTO)

2. Controller valida e converte
   └─> PatientAssessmentState (inicial)

3. Service inicia orquestração
   ├─> Gera sessionId (UUID)
   ├─> Armazena em activeSessions
   └─> Executa fluxo de agentes

4. TriageAgent processa
   ├─> Cria prompt com contexto
   ├─> Chama ChatModel (Qwen 3)
   ├─> Parse resposta JSON
   └─> Atualiza state com RiskAssessment

5. PharmacistAgent processa
   ├─> Usa contexto + resultado da triagem
   ├─> Chama ChatModel
   ├─> Parse resposta JSON
   └─> Atualiza state com PharmacyAnalysis

6. ExamAgent processa
   ├─> Usa contexto completo
   ├─> Chama ChatModel
   ├─> Parse resposta JSON
   └─> Atualiza state com ExamRecommendations

7. EMRCommsAgent processa
   ├─> Consolida todas as informações
   ├─> Chama ChatModel
   ├─> Parse resposta JSON
   └─> Atualiza state com FHIRDocumentation

8. Service pausa execução
   ├─> state.status = "AWAITING_APPROVAL"
   └─> Retorna para cliente

9. Cliente envia POST /approve/{sessionId}
   └─> ApprovalRequest (DTO)

10. Service processa aprovação
    ├─> Valida sessionId
    ├─> Atualiza state.approvalStatus
    └─> state.status = "COMPLETED" ou "REJECTED"

11. Cliente consulta GET /status/{sessionId}
    └─> Recebe AssessmentResponse completo
```

## Integração com LLM

### Spring AI + Ollama

**Configuração:**
```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        options:
          model: qwen2.5:3b
          temperature: 0.1
```

**Uso:**
```java
@Component
public class TriageAgent {
    private final ChatModel chatModel;
    
    public RiskAssessment assessPatient(PatientAssessmentState state) {
        Prompt prompt = promptTemplate.create(variables);
        String response = chatModel.call(prompt)
            .getResult()
            .getOutput()
            .getContent();
        return parseRiskAssessment(response);
    }
}
```

**Características:**
- Injeção de dependência do ChatModel
- Configuração centralizada
- Suporte a múltiplos modelos
- Fallback e retry (futuro)

## Considerações de Escalabilidade

### Limitações Atuais

1. **Armazenamento em memória**
   - Sessões perdidas em restart
   - Não suporta múltiplas instâncias
   - Limite de memória

2. **Processamento síncrono**
   - Cliente aguarda todo o fluxo
   - Timeout em casos complexos
   - Sem paralelização

3. **Sem cache**
   - Chamadas repetidas ao LLM
   - Latência alta

### Melhorias Futuras

1. **Persistência**
   ```
   ConcurrentHashMap → PostgreSQL
   + Redis para cache de sessões
   + Event Store para auditoria
   ```

2. **Processamento assíncrono**
   ```
   REST API → Message Queue (RabbitMQ/Kafka)
   + Workers para processar agentes
   + WebSocket para notificações em tempo real
   ```

3. **Cache inteligente**
   ```
   + Cache de respostas do LLM
   + Cache de análises farmacêuticas
   + Invalidação baseada em contexto
   ```

4. **Balanceamento de carga**
   ```
   + Múltiplas instâncias da aplicação
   + Load balancer (Nginx/HAProxy)
   + Session affinity ou sessões distribuídas
   ```

## Segurança

### Implementações Atuais

- Validação de entrada (Jakarta Validation)
- Exception handling global
- Logs estruturados

### Melhorias Necessárias

1. **Autenticação e Autorização**
   - OAuth2 / JWT
   - RBAC (Role-Based Access Control)
   - API Keys para integrações

2. **Criptografia**
   - HTTPS obrigatório
   - Dados sensíveis criptografados em repouso
   - Tokens de sessão seguros

3. **Auditoria**
   - Log de todas as operações
   - Rastreabilidade de decisões
   - Conformidade HIPAA/LGPD

4. **Rate Limiting**
   - Proteção contra abuso
   - Throttling por usuário/IP
   - Circuit breaker

## Monitoramento e Observabilidade

### Recomendações

1. **Métricas** (Micrometer + Prometheus)
   - Tempo de resposta por agente
   - Taxa de aprovação/rejeição
   - Uso de recursos

2. **Tracing** (OpenTelemetry)
   - Rastreamento de requisições
   - Visualização de fluxo de agentes
   - Identificação de gargalos

3. **Logging** (SLF4J + ELK Stack)
   - Logs estruturados (JSON)
   - Correlação de logs por sessionId
   - Alertas em erros críticos

4. **Health Checks**
   - Spring Actuator
   - Verificação de conectividade com Ollama
   - Status de agentes

## Conclusão

A arquitetura do **Health Multi-Agent System** demonstra uma implementação moderna e escalável de sistemas multi-agente usando Spring AI e LangGraph4j. O design modular permite fácil extensão, manutenção e evolução para ambientes de produção com requisitos de alta disponibilidade e conformidade regulatória.

