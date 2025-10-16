# Guia de Integração - Frontend + Backend

Este guia explica como executar o **Health Multi-Agent System** completo (frontend + backend).

## 📦 Componentes do Sistema

### 1. Backend (Spring Boot)
- **Localização:** `health-multiagent-system/`
- **Tecnologia:** Java 21, Spring Boot 3.5, Spring AI, LangGraph4j
- **Porta:** 8080
- **Endpoints:** `/api/health-assessment/*`

### 2. Frontend (React)
- **Localização:** `health-multiagent-ui/`
- **Tecnologia:** React 19, Vite 6, Tailwind CSS 4
- **Porta:** 5173
- **Comunicação:** REST API com backend

## 🚀 Execução Passo a Passo

### Pré-requisitos

#### Backend
- ✅ Java 21 (SDKMAN instalado)
- ✅ Maven 3.9+
- ✅ Ollama rodando com modelo Qwen 2.5 (3b ou 8b)

#### Frontend
- ✅ Node.js 22+
- ✅ pnpm

### Passo 1: Iniciar Ollama

```bash
# Verificar se Ollama está rodando
curl http://localhost:11434/api/tags

# Se não estiver, iniciar Ollama
ollama serve &

# Baixar modelo Qwen 2.5 (se ainda não tiver)
ollama pull qwen2.5:3b
# ou
ollama pull qwen2.5:8b
```

### Passo 2: Iniciar Backend

```bash
# Navegar para diretório do backend
cd /home/ubuntu/health-multiagent-system

# Compilar projeto
source "$HOME/.sdkman/bin/sdkman-init.sh"
mvn clean package -DskipTests

# Executar aplicação
java -jar target/health-multiagent-system-1.0.0.jar

# Ou usar Maven diretamente
mvn spring-boot:run
```

**Verificar se backend está rodando:**
```bash
curl http://localhost:8080/actuator/health
```

**Acessar Swagger UI:**
```
http://localhost:8080/api/swagger-ui.html
```

### Passo 3: Iniciar Frontend

**Em outro terminal:**

```bash
# Navegar para diretório do frontend
cd /home/ubuntu/health-multiagent-ui

# Instalar dependências (primeira vez)
pnpm install

# Executar em modo desenvolvimento
pnpm dev
```

**Acessar aplicação:**
```
http://localhost:5173
```

## 🔧 Configuração

### Backend: application.yml

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
```

### Frontend: .env

```env
VITE_API_BASE_URL=http://localhost:8080/api/health-assessment
VITE_POLLING_INTERVAL=2000
```

## 🧪 Testar Integração

### Teste 1: Fluxo Normal

1. **Abra o frontend:** `http://localhost:5173`
2. **Preencha os dados:**
   - ID: `PAT-001`
   - Sintomas: `Febre alta há 3 dias, dor de cabeça intensa`
   - Histórico: `Hipertensão`
   - Medicações: `Losartana 50mg`
3. **Clique em "Enviar para Análise"**
4. **Aguarde processamento** (badge "Processando" aparece)
5. **Revise resultados** nas abas Resumo/Detalhes/FHIR
6. **Clique em "Aprovar Documentação"**
7. **Verifique status** muda para "Aprovado" (verde)

### Teste 2: Fluxo com Reprocessamento

1. **Abra o frontend:** `http://localhost:5173`
2. **Preencha os dados:**
   - ID: `PAT-002`
   - Sintomas: `Dor abdominal intensa, náusea, febre baixa`
   - Histórico: `Colecistite prévia há 2 anos`
   - Medicações: `Omeprazol 20mg`
3. **Clique em "Enviar para Análise"**
4. **Aguarde processamento**
5. **Revise resultados**
6. **Escreva feedback nos comentários:**
   ```
   A avaliação não considerou adequadamente o histórico de colecistite. 
   Recomendo incluir ultrassom abdominal como exame prioritário e 
   reavaliar o nível de risco.
   ```
7. **Clique em "Rejeitar e Reprocessar"**
8. **Badge muda** para "Reprocessando" (azul)
9. **Alert aparece** mostrando iteração e feedback
10. **Aguarde reprocessamento**
11. **Revise resultados atualizados**
12. **Verifique se ultrassom** foi adicionado aos exames
13. **Clique em "Aprovar Documentação"**
14. **Verifique status** muda para "Aprovado"

### Teste 3: Via cURL (Backend Direto)

```bash
# 1. Submeter sintomas
curl -X POST http://localhost:8080/api/health-assessment/symptoms \
  -H "Content-Type: application/json" \
  -d '{
    "patientId": "PAT-003",
    "symptoms": "Tosse seca persistente, falta de ar",
    "medicalHistory": "Fumante há 20 anos",
    "currentMedications": []
  }'

# Copie o sessionId retornado

# 2. Verificar status
curl http://localhost:8080/api/health-assessment/status/{sessionId}

# 3. Aprovar
curl -X POST http://localhost:8080/api/health-assessment/approve/{sessionId} \
  -H "Content-Type: application/json" \
  -d '{
    "decision": "APPROVED",
    "comments": "Avaliação adequada"
  }'
```

## 🐛 Troubleshooting

### Backend não inicia

**Erro:** `Connection refused: localhost:11434`
- **Solução:** Iniciar Ollama: `ollama serve &`

**Erro:** `Model qwen2.5:3b not found`
- **Solução:** Baixar modelo: `ollama pull qwen2.5:3b`

**Erro:** `Port 8080 already in use`
- **Solução:** Matar processo: `lsof -ti:8080 | xargs kill -9`

### Frontend não conecta ao backend

**Erro:** `Failed to fetch`
- **Solução 1:** Verificar se backend está rodando: `curl http://localhost:8080/actuator/health`
- **Solução 2:** Verificar URL em `.env`: `VITE_API_BASE_URL=http://localhost:8080/api/health-assessment`
- **Solução 3:** Verificar CORS no backend (já configurado)

**Erro:** `CORS policy`
- **Solução:** Backend já tem CORS configurado para `http://localhost:5173`

### Polling não funciona

**Sintoma:** Status não atualiza automaticamente
- **Solução:** Clicar no botão de refresh manual (ícone de seta circular)
- **Verificar:** Console do navegador para erros

### Reprocessamento não funciona

**Sintoma:** Após rejeitar, status não muda para "Reprocessando"
- **Solução 1:** Verificar logs do backend
- **Solução 2:** Verificar se comentários foram preenchidos
- **Solução 3:** Verificar se não atingiu limite de 3 iterações

## 📊 Monitoramento

### Logs do Backend

```bash
# Ver logs em tempo real
tail -f logs/application.log

# Ou se estiver rodando via Maven
# Os logs aparecem no terminal
```

### Console do Frontend

Abra DevTools do navegador (F12) e vá para a aba Console para ver:
- Requisições HTTP
- Erros de comunicação
- Status de polling

### Endpoints de Monitoramento

```bash
# Health check
curl http://localhost:8080/actuator/health

# Métricas
curl http://localhost:8080/actuator/metrics

# Info
curl http://localhost:8080/actuator/info
```

## 🔄 Fluxo Completo de Dados

```
┌─────────────────┐
│   Frontend      │
│  (React/Vite)   │
│  Port: 5173     │
└────────┬────────┘
         │
         │ HTTP POST /symptoms
         │ {patientId, symptoms, ...}
         ▼
┌─────────────────┐
│    Backend      │
│  (Spring Boot)  │
│  Port: 8080     │
└────────┬────────┘
         │
         │ Executa 4 Agentes
         ├─> Triage Agent
         ├─> Pharmacist Agent
         ├─> Exam Agent
         └─> EMR/Comms Agent
         │
         │ Chama Ollama
         ▼
┌─────────────────┐
│     Ollama      │
│  (Qwen 2.5)     │
│  Port: 11434    │
└─────────────────┘
         │
         │ Retorna análise
         ▼
┌─────────────────┐
│    Backend      │
│ Status: AWAITING│
│    _APPROVAL    │
└────────┬────────┘
         │
         │ HTTP Response
         │ {sessionId, status, data}
         ▼
┌─────────────────┐
│   Frontend      │
│ Exibe resultados│
│ Aguarda decisão │
└────────┬────────┘
         │
         │ Médico decide:
         ├─> APPROVED → Status: COMPLETED
         │
         └─> REJECTED → Reprocessamento
             │
             │ HTTP POST /approve
             │ {decision: REJECTED, comments}
             ▼
         ┌─────────────────┐
         │    Backend      │
         │ Reprocessa com  │
         │    feedback     │
         └────────┬────────┘
                  │
                  │ Executa 4 Agentes novamente
                  │ (com feedback incorporado)
                  ▼
         ┌─────────────────┐
         │   Frontend      │
         │ Exibe resultados│
         │  atualizados    │
         └─────────────────┘
```

## 📝 Checklist de Execução

### Antes de Iniciar

- [ ] Java 21 instalado
- [ ] Maven instalado
- [ ] Node.js 22+ instalado
- [ ] pnpm instalado
- [ ] Ollama instalado
- [ ] Modelo Qwen 2.5 baixado

### Inicialização

- [ ] Ollama rodando (`ollama serve`)
- [ ] Backend compilado (`mvn clean package`)
- [ ] Backend rodando (porta 8080)
- [ ] Frontend dependências instaladas (`pnpm install`)
- [ ] Frontend rodando (porta 5173)

### Verificação

- [ ] Backend health check OK
- [ ] Frontend carrega sem erros
- [ ] Swagger UI acessível
- [ ] Console do navegador sem erros

### Teste

- [ ] Fluxo normal funciona
- [ ] Fluxo de reprocessamento funciona
- [ ] Polling automático funciona
- [ ] Badges de status corretos

## 🎯 Próximos Passos

### Melhorias Sugeridas

1. **Autenticação:**
   - Implementar login de médicos
   - JWT tokens
   - Roles e permissões

2. **Persistência:**
   - Banco de dados (PostgreSQL)
   - Histórico de avaliações
   - Auditoria completa

3. **Notificações:**
   - WebSocket para atualizações em tempo real
   - Notificações push
   - Email para médicos

4. **Exportação:**
   - PDF de relatórios
   - Exportação FHIR completa
   - Integração com sistemas hospitalares

5. **Analytics:**
   - Dashboard de métricas
   - Tempo médio de processamento
   - Taxa de reprocessamento

## 📚 Documentação Adicional

- **Backend:** `health-multiagent-system/README.md`
- **Frontend:** `health-multiagent-ui/README.md`
- **Reprocessamento:** `health-multiagent-system/REPROCESSING.md`
- **Arquitetura:** `health-multiagent-system/ARCHITECTURE.md`
- **Quick Start:** `health-multiagent-system/QUICKSTART.md`

## 🤝 Suporte

Para problemas ou dúvidas:
1. Verifique os logs do backend e frontend
2. Consulte a documentação específica de cada componente
3. Verifique se todos os pré-requisitos estão instalados
4. Teste os endpoints via cURL para isolar problemas

---

**Sistema pronto para uso!** 🎉

Frontend + Backend totalmente integrados com suporte a:
- ✅ 4 agentes especializados
- ✅ Fluxo HITL (Human-in-the-Loop)
- ✅ Reprocessamento com feedback
- ✅ Polling automático
- ✅ Interface moderna e responsiva

