/**
 * API Service para comunicação com Health Multi-Agent System Backend
 * Base URL: http://localhost:8080/api/health-assessment
 */

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/health-assessment';

/**
 * Submete sintomas do paciente para avaliação
 * @param {Object} patientData - Dados do paciente
 * @param {string} patientData.patientId - ID do paciente
 * @param {string} patientData.symptoms - Sintomas relatados
 * @param {string} patientData.medicalHistory - Histórico médico
 * @param {Array<string>} patientData.currentMedications - Medicações atuais
 * @returns {Promise<Object>} Response da avaliação
 */
export async function submitSymptoms(patientData) {
  try {
    const response = await fetch(`${API_BASE_URL}/symptoms`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        patientId: patientData.patientId,
        symptoms: patientData.symptoms,
        medicalHistory: patientData.medicalHistory || null,
        currentMedications: patientData.currentMedications || []
      }),
    });

    if (!response.ok) {
      const errorData = await response.json();
      throw new Error(errorData.message || 'Erro ao submeter sintomas');
    }

    return await response.json();
  } catch (error) {
    console.error('Error submitting symptoms:', error);
    throw error;
  }
}

/**
 * Consulta o status de uma avaliação
 * @param {string} sessionId - ID da sessão
 * @returns {Promise<Object>} Status da avaliação
 */
export async function getAssessmentStatus(sessionId) {
  try {
    const response = await fetch(`${API_BASE_URL}/status/${sessionId}`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
    });

    if (!response.ok) {
      const errorData = await response.json();
      throw new Error(errorData.message || 'Erro ao consultar status');
    }

    return await response.json();
  } catch (error) {
    console.error('Error getting assessment status:', error);
    throw error;
  }
}

/**
 * Aprova ou rejeita uma avaliação
 * @param {string} sessionId - ID da sessão
 * @param {string} decision - 'APPROVED' ou 'REJECTED'
 * @param {string} comments - Comentários do médico
 * @returns {Promise<Object>} Response da aprovação
 */
export async function approveAssessment(sessionId, decision, comments) {
  try {
    const response = await fetch(`${API_BASE_URL}/approve/${sessionId}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        decision,
        comments: comments || ''
      }),
    });

    if (!response.ok) {
      const errorData = await response.json();
      throw new Error(errorData.message || 'Erro ao processar aprovação');
    }

    return await response.json();
  } catch (error) {
    console.error('Error approving assessment:', error);
    throw error;
  }
}

/**
 * Polling para verificar status da avaliação
 * @param {string} sessionId - ID da sessão
 * @param {Function} onUpdate - Callback chamado a cada atualização
 * @param {number} interval - Intervalo em ms (padrão: 2000)
 * @returns {Function} Função para parar o polling
 */
export function pollAssessmentStatus(sessionId, onUpdate, interval = 2000) {
  let isPolling = true;
  
  const poll = async () => {
    if (!isPolling) return;
    
    try {
      const status = await getAssessmentStatus(sessionId);
      onUpdate(status);
      
      // Continuar polling se ainda estiver processando
      if (status.status === 'PROCESSING' || status.status === 'REPROCESSING') {
        setTimeout(poll, interval);
      }
    } catch (error) {
      console.error('Polling error:', error);
      onUpdate({ status: 'ERROR', message: error.message });
    }
  };
  
  poll();
  
  // Retorna função para parar o polling
  return () => {
    isPolling = false;
  };
}

/**
 * Formata medicações de string para array
 * @param {string} medicationsString - String com medicações separadas por vírgula ou linha
 * @returns {Array<string>} Array de medicações
 */
export function parseMedications(medicationsString) {
  if (!medicationsString || medicationsString.trim() === '') {
    return [];
  }
  
  // Divide por vírgula ou quebra de linha
  return medicationsString
    .split(/[,\n]/)
    .map(med => med.trim())
    .filter(med => med.length > 0);
}

/**
 * Mapeia status do backend para badge variant
 * @param {string} status - Status da avaliação
 * @returns {string} Variant do badge
 */
export function getStatusVariant(status) {
  switch (status) {
    case 'COMPLETED':
      return 'success';
    case 'REJECTED':
      return 'destructive';
    case 'AWAITING_APPROVAL':
      return 'warning';
    case 'PROCESSING':
    case 'REPROCESSING':
      return 'default';
    case 'ERROR':
      return 'destructive';
    default:
      return 'outline';
  }
}

/**
 * Mapeia nível de risco para badge variant
 * @param {string} riskLevel - Nível de risco
 * @returns {string} Variant do badge
 */
export function getRiskVariant(riskLevel) {
  switch (riskLevel) {
    case 'CRITICAL':
    case 'HIGH':
      return 'destructive';
    case 'MEDIUM':
      return 'default';
    case 'LOW':
      return 'secondary';
    default:
      return 'outline';
  }
}

