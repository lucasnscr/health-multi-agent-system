# Guia de Início Rápido

Este guia ajudará você a executar o sistema multi-agente de saúde em poucos minutos.

## Passo 1: Instalar Pré-requisitos

### Instalar Java 21

```bash
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 21.0.2-tem
sdk install maven
```

### Instalar Ollama e Modelo Qwen

```bash
# Instalar Ollama
curl -fsSL https://ollama.com/install.sh | sh

# Baixar modelo Qwen 2.5 3B (mais leve, ~2GB)
ollama pull qwen2.5:3b

# OU baixar modelo 8B (melhor qualidade, ~5GB)
ollama pull qwen2.5:8b

# Iniciar servidor Ollama
ollama serve
```

**Nota:** Mantenha o Ollama rodando em um terminal separado.

## Passo 2: Executar a Aplicação

```bash
# Compilar e executar
cd health-multiagent-system
mvn clean spring-boot:run
```

Aguarde a mensagem: `Started HealthMultiAgentApplication in X seconds`

## Passo 3: Testar a API

### Opção 1: Usar Swagger UI (Recomendado)

1. Abra o navegador em: http://localhost:8080/api/swagger-ui.html
2. Expanda o endpoint `POST /health-assessment/symptoms`
3. Clique em "Try it out"
4. Use o exemplo abaixo:

```json
{
  "patientId": "DEMO-001",
  "symptoms": "Febre alta há 2 dias, dor de cabeça intensa, tosse seca",
  "medicalHistory": "Hipertensão",
  "currentMedications": ["Losartana 50mg"]
}
```

5. Clique em "Execute"
6. Copie o `sessionId` da resposta
7. Use o endpoint `POST /health-assessment/approve/{sessionId}` para aprovar

### Opção 2: Usar cURL

```bash
# 1. Submeter sintomas
curl -X POST http://localhost:8080/api/health-assessment/symptoms \
  -H "Content-Type: application/json" \
  -d '{
    "patientId": "DEMO-001",
    "symptoms": "Febre alta há 2 dias, dor de cabeça intensa, tosse seca",
    "medicalHistory": "Hipertensão",
    "currentMedications": ["Losartana 50mg"]
  }'

# Copie o sessionId da resposta

# 2. Verificar status
curl http://localhost:8080/api/health-assessment/status/SEU_SESSION_ID

# 3. Aprovar documentação
curl -X POST http://localhost:8080/api/health-assessment/approve/SEU_SESSION_ID \
  -H "Content-Type: application/json" \
  -d '{
    "decision": "APPROVED",
    "comments": "Documentação aprovada"
  }'
```

## Passo 4: Entender a Resposta

A resposta conterá:

- **riskLevel**: Nível de risco (LOW, MEDIUM, HIGH, CRITICAL)
- **symptomsSummary**: Resumo dos sintomas analisados
- **drugInteractions**: Interações medicamentosas identificadas
- **recommendedLabExams**: Exames laboratoriais recomendados
- **recommendedImagingExams**: Exames de imagem recomendados
- **fhirDocument**: Documento FHIR gerado
- **communicationText**: Texto de comunicação para profissionais de saúde

## Fluxo Completo

```
1. Cliente → POST /symptoms
   ↓
2. Sistema executa 4 agentes:
   - Triage Agent (avalia risco)
   - Pharmacist Agent (analisa medicações)
   - Exam Agent (recomenda exames)
   - EMR/Comms Agent (gera documentação)
   ↓
3. Sistema → Retorna status AWAITING_APPROVAL
   ↓
4. Cliente → POST /approve/{sessionId}
   ↓
5. Sistema → Retorna status COMPLETED
```

## Solução de Problemas

### Erro: "Connection refused" ao acessar Ollama

**Solução:** Certifique-se de que o Ollama está rodando:
```bash
ollama serve
```

### Erro: "Model not found"

**Solução:** Baixe o modelo:
```bash
ollama pull qwen2.5:3b
```

### Erro: Porta 8080 já em uso

**Solução:** Altere a porta em `application.yml`:
```yaml
server:
  port: 8081
```

### Aplicação muito lenta

**Solução:** Use o modelo menor (3B) ou aumente a memória disponível para o Ollama.

## Próximos Passos

- Explore a documentação completa no [README.md](README.md)
- Customize os prompts dos agentes em `application.yml`
- Implemente persistência em banco de dados
- Adicione autenticação e autorização

## Recursos Adicionais

- **Swagger UI**: http://localhost:8080/api/swagger-ui.html
- **API Docs**: http://localhost:8080/api/v3/api-docs
- **Health Check**: http://localhost:8080/api/actuator/health (se habilitado)

## Suporte

Para dúvidas ou problemas, consulte:
- [README.md](README.md) - Documentação completa
- [Issues no GitHub](https://github.com/seu-repo/issues) - Reportar bugs

