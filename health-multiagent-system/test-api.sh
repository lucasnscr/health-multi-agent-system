#!/bin/bash

# Script de teste da API Health Multi-Agent System
# Uso: ./test-api.sh

set -e

API_BASE_URL="http://localhost:8080/api/health-assessment"

echo "========================================="
echo "Health Multi-Agent System - API Test"
echo "========================================="
echo ""

# Cores para output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Teste 1: Submeter sintomas
echo -e "${BLUE}[1/3] Submetendo sintomas do paciente...${NC}"
echo ""

RESPONSE=$(curl -s -X POST "${API_BASE_URL}/symptoms" \
  -H "Content-Type: application/json" \
  -d '{
    "patientId": "TEST-001",
    "symptoms": "Febre alta há 3 dias, dor de cabeça intensa, dor no corpo, náusea ocasional",
    "medicalHistory": "Hipertensão controlada, diabetes tipo 2",
    "currentMedications": ["Losartana 50mg", "Metformina 850mg"]
  }')

echo "$RESPONSE" | jq '.'

# Extrair sessionId
SESSION_ID=$(echo "$RESPONSE" | jq -r '.sessionId')

if [ "$SESSION_ID" == "null" ] || [ -z "$SESSION_ID" ]; then
    echo -e "${YELLOW}Erro: Não foi possível obter sessionId${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}✓ Session ID obtido: $SESSION_ID${NC}"
echo ""

# Aguardar um momento
sleep 2

# Teste 2: Verificar status
echo -e "${BLUE}[2/3] Verificando status da avaliação...${NC}"
echo ""

STATUS_RESPONSE=$(curl -s -X GET "${API_BASE_URL}/status/${SESSION_ID}")
echo "$STATUS_RESPONSE" | jq '.'

echo ""
echo -e "${GREEN}✓ Status verificado${NC}"
echo ""

# Aguardar um momento
sleep 2

# Teste 3: Aprovar documentação
echo -e "${BLUE}[3/3] Aprovando documentação FHIR...${NC}"
echo ""

APPROVAL_RESPONSE=$(curl -s -X POST "${API_BASE_URL}/approve/${SESSION_ID}" \
  -H "Content-Type: application/json" \
  -d '{
    "decision": "APPROVED",
    "comments": "Documentação revisada e aprovada para teste"
  }')

echo "$APPROVAL_RESPONSE" | jq '.'

echo ""
echo -e "${GREEN}✓ Documentação aprovada${NC}"
echo ""

# Resumo
echo "========================================="
echo -e "${GREEN}Teste concluído com sucesso!${NC}"
echo "========================================="
echo ""
echo "Resumo:"
echo "- Session ID: $SESSION_ID"
echo "- Status Final: $(echo "$APPROVAL_RESPONSE" | jq -r '.status')"
echo "- Risk Level: $(echo "$APPROVAL_RESPONSE" | jq -r '.data.riskLevel')"
echo ""
echo "Para ver detalhes completos, acesse:"
echo "http://localhost:8080/api/swagger-ui.html"
echo ""

