#!/bin/bash

# Script de teste do fluxo de reprocessamento
# Demonstra o ciclo de rejeição e reprocessamento com feedback do médico

set -e

API_BASE_URL="http://localhost:8080/api/health-assessment"

echo "========================================="
echo "Health Multi-Agent - Reprocessing Test"
echo "========================================="
echo ""

# Cores para output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Passo 1: Submeter sintomas
echo -e "${BLUE}[1/6] Submetendo sintomas do paciente...${NC}"
echo ""

RESPONSE=$(curl -s -X POST "${API_BASE_URL}/symptoms" \
  -H "Content-Type: application/json" \
  -d '{
    "patientId": "REPROCESS-TEST-001",
    "symptoms": "Dor abdominal intensa no quadrante superior direito, náusea, febre baixa",
    "medicalHistory": "Colecistite prévia há 2 anos",
    "currentMedications": ["Omeprazol 20mg"]
  }')

echo "$RESPONSE" | jq '.'

SESSION_ID=$(echo "$RESPONSE" | jq -r '.sessionId')

if [ "$SESSION_ID" == "null" ] || [ -z "$SESSION_ID" ]; then
    echo -e "${RED}Erro: Não foi possível obter sessionId${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}✓ Session ID: $SESSION_ID${NC}"
echo ""
sleep 2

# Passo 2: Verificar status inicial
echo -e "${BLUE}[2/6] Verificando status da primeira avaliação...${NC}"
echo ""

STATUS_RESPONSE=$(curl -s -X GET "${API_BASE_URL}/status/${SESSION_ID}")
echo "$STATUS_RESPONSE" | jq '.message, .data.riskLevel, .data.reprocessingCount'

echo ""
echo -e "${GREEN}✓ Primeira avaliação concluída${NC}"
echo ""
sleep 2

# Passo 3: REJEITAR com feedback do médico
echo -e "${YELLOW}[3/6] Médico rejeitando avaliação e fornecendo feedback...${NC}"
echo ""

REJECTION_RESPONSE=$(curl -s -X POST "${API_BASE_URL}/approve/${SESSION_ID}" \
  -H "Content-Type: application/json" \
  -d '{
    "decision": "REJECTED",
    "comments": "A avaliação não considerou adequadamente o histórico de colecistite. Recomendo incluir ultrassom abdominal como exame prioritário e reavaliar o nível de risco considerando possível recorrência de colecistite aguda."
  }')

echo "$REJECTION_RESPONSE" | jq '.'

echo ""
echo -e "${YELLOW}✓ Avaliação rejeitada, iniciando reprocessamento${NC}"
echo ""
sleep 3

# Passo 4: Verificar status durante reprocessamento
echo -e "${BLUE}[4/6] Verificando status após reprocessamento...${NC}"
echo ""

STATUS_RESPONSE2=$(curl -s -X GET "${API_BASE_URL}/status/${SESSION_ID}")
echo "$STATUS_RESPONSE2" | jq '.message, .data.riskLevel, .data.reprocessingCount, .data.physicianFeedback'

echo ""
echo -e "${GREEN}✓ Reprocessamento concluído (iteração 1)${NC}"
echo ""
sleep 2

# Passo 5: Verificar se feedback foi incorporado
echo -e "${BLUE}[5/6] Verificando se feedback foi incorporado nos exames...${NC}"
echo ""

echo "$STATUS_RESPONSE2" | jq '.data.recommendedImagingExams'

echo ""
sleep 2

# Passo 6: APROVAR a avaliação reprocessada
echo -e "${GREEN}[6/6] Médico aprovando avaliação reprocessada...${NC}"
echo ""

APPROVAL_RESPONSE=$(curl -s -X POST "${API_BASE_URL}/approve/${SESSION_ID}" \
  -H "Content-Type: application/json" \
  -d '{
    "decision": "APPROVED",
    "comments": "Avaliação reprocessada adequadamente. Ultrassom abdominal incluído e risco reavaliado corretamente."
  }')

echo "$APPROVAL_RESPONSE" | jq '.message, .data.reprocessingCount, .data.assessmentHistory'

echo ""
echo -e "${GREEN}✓ Avaliação aprovada após reprocessamento${NC}"
echo ""

# Resumo
echo "========================================="
echo -e "${GREEN}Teste de Reprocessamento Concluído!${NC}"
echo "========================================="
echo ""
echo "Resumo do Fluxo:"
echo "1. ✓ Avaliação inicial submetida"
echo "2. ✓ Médico rejeitou com feedback específico"
echo "3. ✓ Sistema reprocessou incorporando feedback"
echo "4. ✓ Médico aprovou avaliação reprocessada"
echo ""
echo "Detalhes:"
echo "- Session ID: $SESSION_ID"
echo "- Iterações de reprocessamento: $(echo "$APPROVAL_RESPONSE" | jq -r '.data.reprocessingCount')"
echo "- Status final: $(echo "$APPROVAL_RESPONSE" | jq -r '.status')"
echo ""
echo "Para testar múltiplas rejeições (até 3 iterações),"
echo "execute o script novamente e rejeite mais vezes."
echo ""

