# Health Multi-Agent System

Sistema de avaliação clínica com inteligência artificial multi-agente, implementando fluxo HITL (Human-in-the-Loop) e reprocessamento iterativo com feedback médico.

![Version](https://img.shields.io/badge/version-1.0.0-blue.svg)
![Java](https://img.shields.io/badge/Java-21-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.0-green.svg)
![React](https://img.shields.io/badge/React-19.1-blue.svg)
![License](https://img.shields.io/badge/license-MIT-green.svg)

## 📋 Visão Geral

O **Health Multi-Agent System** é uma solução full-stack que combina múltiplos agentes especializados de IA para realizar avaliações clínicas completas, com supervisão médica e capacidade de reprocessamento iterativo baseado em feedback.

### Arquitetura do Sistema

```
┌─────────────────────────────────────────────────────────────────┐
│                        Frontend (React)                         │
│  Interface web moderna com polling automático e visualização    │
│                    de resultados em tempo real                  │
└────────────────────────────┬────────────────────────────────────┘
                             │ REST API
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                     Backend (Spring Boot)                       │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │              LangGraph4j Orchestration                   │  │
│  │  ┌────────────┐  ┌────────────┐  ┌────────────┐        │  │
│  │  │   Triage   │→ │ Pharmacist │→ │    Exam    │→       │  │
│  │  │   Agent    │  │   Agent    │  │   Agent    │        │  │
│  │  └────────────┘  └────────────┘  └────────────┘        │  │
│  │                                                          │  │
│  │  ┌────────────────────────────────────────────┐        │  │
│  │  │         EMR/Comms Agent (HITL)             │        │  │
│  │  │  • Gera documentação FHIR                  │        │  │
│  │  │  • InterruptNode para aprovação médica     │        │  │
│  │  │  • Suporta reprocessamento com feedback    │        │  │
│  │  └────────────────────────────────────────────┘        │  │
│  └──────────────────────────────────────────────────────────┘  │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │  Ollama + Qwen  │
                    │   2.5 (3b/8b)   │
                    └─────────────────┘
```

## 🌟 Características Principais

### Backend (Spring Boot + LangGraph4j)

#### 1. Arquitetura Multi-Agente
- **4 agentes especializados** trabalhando em sequência
- **Agent Handoff Pattern** para transferência de contexto
- **Estado compartilhado** (`PatientAssessmentState`) entre agentes

#### 2. Agentes Implementados

| Agente | Responsabilidade | Saída Principal |
|--------|------------------|-----------------|
| **Triage Agent** | Avalia sintomas e determina nível de risco | `riskLevel`, `symptomsSummary`, `triageRecommendations` |
| **Pharmacist Agent** | Analisa medicações e identifica interações | `drugInteractions`, `contraindications`, `pharmacistRecommendations` |
| **Exam Agent** | Recomenda exames laboratoriais e de imagem | `recommendedLabExams`, `recommendedImagingExams` |
| **EMR/Comms Agent** | Gera documentação FHIR e comunicações | `fhirDocument`, `communicationText` |

#### 3. HITL (Human-in-the-Loop)
- **InterruptNode** no EMR/Comms Agent
- Pausa execução antes de finalizar
- Aguarda aprovação/rejeição do médico
- Endpoint dedicado para decisão

#### 4. Reprocessamento Inteligente
- **Até 3 iterações** de reprocessamento
- **Feedback do médico** incorporado nos prompts
- **Histórico completo** de avaliações preservado
- **Reprocessamento automático** após rejeição

#### 5. Integração com IA
- **Spring AI** para abstração de LLMs
- **Ollama** como provider local
- **Qwen 2.5** (3B ou 8B) como modelo
- **Temperatura 0.1** para respostas consistentes

### Frontend (React + Vite + Tailwind)

#### 1. Interface Moderna
- **Design responsivo** com Tailwind CSS
- **Componentes shadcn/ui** de alta qualidade
- **Gradientes e animações** suaves
- **Tema claro** otimizado para ambiente clínico

#### 2. Funcionalidades da UI

##### Entrada de Dados
- Formulário estruturado e validado
- Campos: ID, sintomas, histórico, medicações
- Preview do request JSON
- Desabilitação após submissão

##### Visualização de Resultados
- **3 abas organizadas:**
  - **Resumo:** Risco, sintomas, exames
  - **Detalhes:** Interações, recomendações, histórico
  - **FHIR:** Documentação JSON formatada

##### Badges Dinâmicos
- 🟢 **COMPLETED** - Aprovado
- 🔴 **REJECTED** - Rejeitado
- 🟡 **AWAITING_APPROVAL** - Aguardando
- ⚪ **PROCESSING** - Processando
- 🔵 **REPROCESSING** - Reprocessando

##### Polling Automático
- Atualização a cada 2 segundos
- Refresh manual disponível
- Timeout de 2 minutos

##### Fluxo de Reprocessamento Visual
- Alert especial mostrando iteração
- Exibição do feedback anterior
- Contador de iterações
- Aviso na última iteração

## 🚀 Instalação e Execução

### Pré-requisitos

#### Backend
- Java 21 (via SDKMAN)
- Maven 3.9+
- Ollama com Qwen 2.5 (3b ou 8b)

#### Frontend
- Node.js 22+
- pnpm (recomendado)

### Instalação Completa

#### 1. Instalar Ollama e Modelo

```bash
# Instalar Ollama (se ainda não tiver)
curl -fsSL https://ollama.ai/install.sh | sh

# Iniciar Ollama
ollama serve &

# Baixar modelo Qwen 2.5
ollama pull qwen2.5:3b
# ou para modelo maior
ollama pull qwen2.5:8b
```

#### 2. Configurar e Executar Backend

```bash
# Clonar/extrair projeto
cd health-multiagent-system

# Compilar
source "$HOME/.sdkman/bin/sdkman-init.sh"
mvn clean package -DskipTests

# Executar
mvn spring-boot:run

# Ou via JAR
java -jar target/health-multiagent-system-1.0.0.jar
```

**Verificar backend:**
```bash
curl http://localhost:8080/actuator/health
```

**Acessar Swagger:**
```
http://localhost:8080/api/swagger-ui.html
```

#### 3. Configurar e Executar Frontend

```bash
# Navegar para diretório do frontend
cd health-multiagent-ui

# Instalar dependências
pnpm install

# Executar em desenvolvimento
pnpm dev
```

**Acessar aplicação:**
```
http://localhost:5173
```

## 📖 Uso do Sistema

### Fluxo Normal (Aprovação Direta)

1. **Acesse** `http://localhost:5173`
2. **Preencha os dados do paciente:**
   ```
   ID: PAT-001
   Sintomas: Febre alta há 3 dias, dor de cabeça intensa, tosse seca
   Histórico: Hipertensão arterial
   Medicações: Losartana 50mg, Hidroclorotiazida 25mg
   ```
3. **Clique** em "Enviar para Análise"
4. **Aguarde** processamento (badge "Processando" com spinner)
5. **Revise** resultados nas 3 abas
6. **Aprove** clicando em "Aprovar Documentação"
7. **Confirme** status "Aprovado" (verde)

### Fluxo com Reprocessamento

1. **Acesse** `http://localhost:5173`
2. **Preencha os dados:**
   ```
   ID: PAT-002
   Sintomas: Dor abdominal intensa no quadrante superior direito, náusea, febre baixa
   Histórico: Colecistite prévia há 2 anos
   Medicações: Omeprazol 20mg
   ```
3. **Clique** em "Enviar para Análise"
4. **Revise** resultados iniciais
5. **Escreva feedback específico:**
   ```
   A avaliação não considerou adequadamente o histórico de colecistite. 
   Recomendo incluir ultrassom abdominal como exame prioritário e 
   reavaliar o nível de risco considerando possível recorrência.
   ```
6. **Clique** em "Rejeitar e Reprocessar"
7. **Observe:**
   - Badge muda para "Reprocessando" (azul)
   - Alert aparece mostrando "Iteração 1 de 3"
   - Feedback do médico é exibido
8. **Aguarde** reprocessamento automático
9. **Revise** resultados atualizados:
   - Ultrassom abdominal adicionado
   - Nível de risco reavaliado
10. **Aprove** ou **rejeite novamente** (até 3 iterações)

### Teste via cURL (Backend Direto)

```bash
# 1. Submeter sintomas
curl -X POST http://localhost:8080/api/health-assessment/symptoms \
  -H "Content-Type: application/json" \
  -d '{
    "patientId": "PAT-003",
    "symptoms": "Tosse seca persistente, falta de ar ao subir escadas",
    "medicalHistory": "Fumante há 20 anos, 1 maço/dia",
    "currentMedications": []
  }'

# Copiar sessionId da resposta

# 2. Verificar status
curl http://localhost:8080/api/health-assessment/status/{sessionId}

# 3. Rejeitar com feedback
curl -X POST http://localhost:8080/api/health-assessment/approve/{sessionId} \
  -H "Content-Type: application/json" \
  -d '{
    "decision": "REJECTED",
    "comments": "Considerar histórico de tabagismo. Adicionar raio-X de tórax e espirometria."
  }'

# 4. Verificar reprocessamento
curl http://localhost:8080/api/health-assessment/status/{sessionId}

# 5. Aprovar
curl -X POST http://localhost:8080/api/health-assessment/approve/{sessionId} \
  -H "Content-Type: application/json" \
  -d '{
    "decision": "APPROVED",
    "comments": "Avaliação reprocessada adequadamente."
  }'
```

## 🔧 Configuração

### Backend: `application.yml`

```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        options:
          model: qwen2.5:3b  # ou qwen2.5:8b
          temperature: 0.1
          
server:
  port: 8080

health:
  multiagent:
    reprocessing:
      max-iterations: 3
      enabled: true
    session:
      timeout-minutes: 30
```

### Frontend: `.env`

```env
# Backend API Base URL
VITE_API_BASE_URL=http://localhost:8080/api/health-assessment

# Polling interval in milliseconds
VITE_POLLING_INTERVAL=2000
```

## 📁 Estrutura do Projeto

```
health-multiagent-project/
├── health-multiagent-system/          # Backend Spring Boot
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/health/multiagent/
│   │   │   │   ├── agent/             # Agentes especializados
│   │   │   │   │   ├── TriageAgent.java
│   │   │   │   │   ├── PharmacistAgent.java
│   │   │   │   │   ├── ExamAgent.java
│   │   │   │   │   └── EMRCommsAgent.java
│   │   │   │   ├── controller/        # REST Controllers
│   │   │   │   │   └── HealthAssessmentController.java
│   │   │   │   ├── service/           # Serviços
│   │   │   │   │   └── HealthAssessmentService.java
│   │   │   │   ├── model/             # Modelos de dados
│   │   │   │   │   ├── PatientAssessmentState.java
│   │   │   │   │   ├── SymptomsRequest.java
│   │   │   │   │   └── ApprovalRequest.java
│   │   │   │   └── config/            # Configurações
│   │   │   └── resources/
│   │   │       └── application.yml
│   │   └── test/
│   ├── pom.xml
│   ├── README.md
│   ├── REPROCESSING.md
│   ├── ARCHITECTURE.md
│   └── QUICKSTART.md
│
├── health-multiagent-ui/              # Frontend React
│   ├── src/
│   │   ├── components/
│   │   │   └── ui/                    # Componentes shadcn/ui
│   │   ├── services/
│   │   │   └── api.js                 # Serviço de API
│   │   ├── App.jsx                    # Componente principal
│   │   └── main.jsx
│   ├── .env
│   ├── package.json
│   ├── vite.config.js
│   └── README.md
│
├── README.md                          # Este arquivo
└── GUIA_INTEGRACAO_UI.md             # Guia de execução
```

## 🔌 API REST

### Endpoints Disponíveis

#### 1. Submeter Sintomas

```http
POST /api/health-assessment/symptoms
Content-Type: application/json

{
  "patientId": "PAT-001",
  "symptoms": "Febre alta, dor de cabeça",
  "medicalHistory": "Hipertensão",
  "currentMedications": ["Losartana 50mg"]
}
```

**Response:**
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "PROCESSING",
  "message": "Assessment in progress. Processing through 4 specialized agents."
}
```

#### 2. Consultar Status

```http
GET /api/health-assessment/status/{sessionId}
```

**Response (AWAITING_APPROVAL):**
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "AWAITING_APPROVAL",
  "message": "Assessment completed. Awaiting physician approval.",
  "data": {
    "riskLevel": "HIGH",
    "symptomsSummary": "Paciente apresenta febre alta...",
    "recommendedLabExams": ["Hemograma completo", "Hemoculturas"],
    "recommendedImagingExams": ["Raio-X de tórax"],
    "drugInteractions": [],
    "triageRecommendations": "Avaliação médica urgente...",
    "pharmacistRecommendations": "Monitorar função renal...",
    "communicationText": "### Avaliação Clínica...",
    "fhirDocument": {...},
    "reprocessingCount": 0
  }
}
```

#### 3. Aprovar/Rejeitar

```http
POST /api/health-assessment/approve/{sessionId}
Content-Type: application/json

{
  "decision": "APPROVED",  // ou "REJECTED"
  "comments": "Documentação aprovada"
}
```

**Response (APPROVED):**
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "COMPLETED",
  "message": "Assessment completed and approved.",
  "data": {...}
}
```

**Response (REJECTED - Reprocessamento):**
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "REPROCESSING",
  "message": "Reprocessing assessment with physician feedback...",
  "data": {
    "reprocessingCount": 1,
    "physicianFeedback": "Considerar histórico...",
    "assessmentHistory": [
      "Iteration 0 - Risk: MEDIUM, Exams: 2, Physician Feedback: ..."
    ],
    ...
  }
}
```

## 🧪 Testes

### Teste Automatizado de Reprocessamento

```bash
cd health-multiagent-system
./test-reprocessing.sh
```

Este script:
1. Submete sintomas de teste
2. Rejeita com feedback específico
3. Verifica reprocessamento automático
4. Aprova avaliação reprocessada
5. Exibe histórico completo

### Testes Unitários

```bash
cd health-multiagent-system
mvn test
```

### Testes de Integração

```bash
cd health-multiagent-system
mvn verify
```

## 📊 Monitoramento

### Health Check

```bash
curl http://localhost:8080/actuator/health
```

### Métricas

```bash
curl http://localhost:8080/actuator/metrics
```

### Logs

```bash
# Backend
tail -f logs/application.log

# Frontend (console do navegador)
# Abrir DevTools (F12) → Console
```

## 🎯 Casos de Uso

### 1. Triagem de Emergência

**Cenário:** Paciente chega ao pronto-socorro com sintomas agudos

**Fluxo:**
1. Enfermeira insere sintomas no sistema
2. Triage Agent determina nível de risco (CRITICAL/HIGH/MEDIUM/LOW)
3. Sistema recomenda exames prioritários
4. Médico revisa e aprova
5. Documentação FHIR gerada automaticamente

### 2. Revisão de Medicações

**Cenário:** Paciente polimedicado com novo sintoma

**Fluxo:**
1. Médico insere sintomas e medicações atuais
2. Pharmacist Agent identifica possíveis interações
3. Sistema alerta sobre contraindicações
4. Médico ajusta prescrição baseado no feedback
5. Reprocessamento com nova lista de medicações

### 3. Investigação Diagnóstica

**Cenário:** Sintomas complexos requerem múltiplos exames

**Fluxo:**
1. Médico insere sintomas e histórico
2. Exam Agent recomenda bateria de exames
3. Médico discorda da priorização
4. Fornece feedback específico
5. Sistema reprocessa e ajusta recomendações
6. Médico aprova plano diagnóstico final

## 🔒 Segurança e Conformidade

### Implementado
- ✅ Validação de entrada de dados
- ✅ Exception handling global
- ✅ CORS configurado
- ✅ Logs de auditoria
- ✅ Session management

### Recomendado para Produção
- [ ] Autenticação JWT
- [ ] Autorização baseada em roles
- [ ] Criptografia de dados sensíveis
- [ ] Rate limiting
- [ ] HTTPS obrigatório
- [ ] Conformidade HIPAA/LGPD
- [ ] Backup automático
- [ ] Disaster recovery

## 🚀 Deploy

### Backend (Spring Boot)

#### Docker

```dockerfile
FROM openjdk:21-jdk-slim
COPY target/health-multiagent-system-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

```bash
docker build -t health-multiagent-backend .
docker run -p 8080:8080 health-multiagent-backend
```

#### Cloud (Heroku/AWS/Azure)

```bash
# Heroku
heroku create health-multiagent-backend
git push heroku main

# AWS Elastic Beanstalk
eb init -p java-21 health-multiagent-backend
eb create health-multiagent-env
eb deploy
```

### Frontend (React)

#### Build

```bash
cd health-multiagent-ui
pnpm build
```

#### Deploy

**Vercel:**
```bash
vercel deploy
```

**Netlify:**
```bash
netlify deploy --prod
```

**Nginx:**
```nginx
server {
    listen 80;
    server_name health-multiagent.com;
    
    location / {
        root /var/www/health-multiagent-ui/dist;
        try_files $uri $uri/ /index.html;
    }
}
```

## 📈 Roadmap

### Versão 1.1 (Próximo Release)
- [ ] Autenticação e autorização
- [ ] Persistência em banco de dados (PostgreSQL)
- [ ] WebSocket para atualizações em tempo real
- [ ] Exportação de relatórios em PDF
- [ ] Histórico de avaliações do paciente

### Versão 1.2
- [ ] Integração com sistemas hospitalares (HL7/FHIR)
- [ ] Suporte multilíngue
- [ ] Dashboard de analytics
- [ ] Machine learning para prever necessidade de reprocessamento
- [ ] Cache inteligente de resultados

### Versão 2.0
- [ ] Agentes adicionais (Radiologia, Patologia)
- [ ] Suporte a múltiplos LLMs
- [ ] Fine-tuning de modelos
- [ ] API GraphQL
- [ ] Mobile app (React Native)

## 🤝 Contribuição

Contribuições são bem-vindas! Para contribuir:

1. Fork o projeto
2. Crie uma branch para sua feature (`git checkout -b feature/nova-feature`)
3. Commit suas mudanças (`git commit -m 'Add nova feature'`)
4. Push para a branch (`git push origin feature/nova-feature`)
5. Abra um Pull Request

### Diretrizes
- Seguir padrões de código existentes
- Adicionar testes para novas funcionalidades
- Atualizar documentação
- Usar commits semânticos

## 📄 Licença

Este projeto está sob a licença MIT. Veja o arquivo `LICENSE` para mais detalhes.

## 👥 Autores

- **Desenvolvedor Principal** - Sistema Multi-Agente e Integração

## 🙏 Agradecimentos

- **LangGraph4j** - Framework de orquestração de agentes
- **Spring AI** - Abstração de LLMs
- **Ollama** - Execução local de modelos
- **Qwen Team** - Modelo de linguagem
- **shadcn/ui** - Componentes UI de alta qualidade

## 📞 Suporte

Para dúvidas, problemas ou sugestões:

- 📧 Email: support@health-multiagent.com
- 🐛 Issues: [GitHub Issues](https://github.com/your-repo/issues)
- 📖 Documentação: [Wiki](https://github.com/your-repo/wiki)
- 💬 Discussões: [GitHub Discussions](https://github.com/your-repo/discussions)

## 📚 Documentação Adicional

- **[REPROCESSING.md](health-multiagent-system/REPROCESSING.md)** - Documentação detalhada do fluxo de reprocessamento
- **[ARCHITECTURE.md](health-multiagent-system/ARCHITECTURE.md)** - Arquitetura técnica do sistema
- **[QUICKSTART.md](health-multiagent-system/QUICKSTART.md)** - Guia de início rápido
- **[GUIA_INTEGRACAO_UI.md](GUIA_INTEGRACAO_UI.md)** - Guia de integração frontend/backend

---

**Desenvolvido com ❤️ para melhorar o atendimento clínico através de IA**

![Health Multi-Agent System](https://via.placeholder.com/800x400/4F46E5/FFFFFF?text=Health+Multi-Agent+System)

