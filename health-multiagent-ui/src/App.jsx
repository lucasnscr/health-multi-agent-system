import { useState } from 'react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card.jsx'
import { Button } from '@/components/ui/button.jsx'
import { Input } from '@/components/ui/input.jsx'
import { Label } from '@/components/ui/label.jsx'
import { Textarea } from '@/components/ui/textarea.jsx'
import { Badge } from '@/components/ui/badge.jsx'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs.jsx'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert.jsx'
import { 
  Activity, 
  AlertCircle, 
  CheckCircle2, 
  Clock, 
  FileText, 
  Stethoscope, 
  User, 
  Pill,
  FlaskConical,
  Send,
  XCircle,
  RefreshCw,
  History
} from 'lucide-react'
import { 
  submitSymptoms, 
  getAssessmentStatus, 
  approveAssessment, 
  parseMedications,
  getRiskVariant 
} from './services/api'
import './App.css'

function App() {
  const [patientData, setPatientData] = useState({
    patientId: '',
    symptoms: '',
    medicalHistory: '',
    currentMedications: ''
  })

  const [sessionId, setSessionId] = useState(null)
  const [assessment, setAssessment] = useState(null)
  const [approvalComments, setApprovalComments] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState(null)

  const handleInputChange = (field, value) => {
    setPatientData(prev => ({ ...prev, [field]: value }))
  }

  const handleSubmitAssessment = async () => {
    setIsLoading(true)
    setError(null)
    
    try {
      const medications = parseMedications(patientData.currentMedications)
      
      const response = await submitSymptoms({
        ...patientData,
        currentMedications: medications
      })
      
      setSessionId(response.sessionId)
      setAssessment(response)
      
      // Se ainda estiver processando, iniciar polling
      if (response.status === 'PROCESSING' || response.status === 'REPROCESSING') {
        startPolling(response.sessionId)
      }
      
    } catch (err) {
      setError(err.message)
    } finally {
      setIsLoading(false)
    }
  }

  const startPolling = async (sid) => {
    const pollInterval = setInterval(async () => {
      try {
        const status = await getAssessmentStatus(sid)
        setAssessment(status)
        
        // Parar polling quando não estiver mais processando
        if (status.status !== 'PROCESSING' && status.status !== 'REPROCESSING') {
          clearInterval(pollInterval)
        }
      } catch (err) {
        console.error('Polling error:', err)
        clearInterval(pollInterval)
      }
    }, 2000)
    
    // Limpar intervalo após 2 minutos
    setTimeout(() => clearInterval(pollInterval), 120000)
  }

  const handleApproval = async (decision) => {
    setIsLoading(true)
    setError(null)
    
    try {
      const response = await approveAssessment(sessionId, decision, approvalComments)
      setAssessment(response)
      setApprovalComments('')
      
      // Se foi rejeitado e está reprocessando, iniciar polling
      if (response.status === 'REPROCESSING') {
        startPolling(sessionId)
      }
      
    } catch (err) {
      setError(err.message)
    } finally {
      setIsLoading(false)
    }
  }

  const handleRefreshStatus = async () => {
    if (!sessionId) return
    
    setIsLoading(true)
    try {
      const status = await getAssessmentStatus(sessionId)
      setAssessment(status)
    } catch (err) {
      setError(err.message)
    } finally {
      setIsLoading(false)
    }
  }

  const handleNewAssessment = () => {
    setSessionId(null)
    setAssessment(null)
    setApprovalComments('')
    setError(null)
    setPatientData({
      patientId: '',
      symptoms: '',
      medicalHistory: '',
      currentMedications: ''
    })
  }

  const getStatusBadge = (status) => {
    switch(status) {
      case 'COMPLETED':
        return <Badge className="bg-green-500 hover:bg-green-600"><CheckCircle2 className="w-3 h-3 mr-1" />Aprovado</Badge>
      case 'REJECTED':
        return <Badge variant="destructive"><XCircle className="w-3 h-3 mr-1" />Rejeitado</Badge>
      case 'AWAITING_APPROVAL':
        return <Badge variant="outline" className="border-yellow-500 text-yellow-700"><Clock className="w-3 h-3 mr-1" />Aguardando Aprovação</Badge>
      case 'PROCESSING':
        return <Badge variant="default"><Clock className="w-3 h-3 mr-1 animate-spin" />Processando</Badge>
      case 'REPROCESSING':
        return <Badge variant="default" className="bg-blue-500"><RefreshCw className="w-3 h-3 mr-1 animate-spin" />Reprocessando</Badge>
      case 'ERROR':
        return <Badge variant="destructive"><AlertCircle className="w-3 h-3 mr-1" />Erro</Badge>
      default:
        return null
    }
  }

  const getRiskLevelText = (level) => {
    switch(level) {
      case 'CRITICAL': return 'CRÍTICO'
      case 'HIGH': return 'ALTO'
      case 'MEDIUM': return 'MÉDIO'
      case 'LOW': return 'BAIXO'
      default: return level
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 via-white to-purple-50 p-6">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <div className="mb-8">
          <div className="flex items-center justify-between mb-2">
            <div className="flex items-center gap-3">
              <div className="p-2 bg-blue-600 rounded-lg">
                <Stethoscope className="w-8 h-8 text-white" />
              </div>
              <div>
                <h1 className="text-4xl font-bold text-gray-900">Health Multi-Agent System</h1>
                <p className="text-gray-600 mt-1">Sistema de Avaliação Clínica com IA Multi-Agente</p>
              </div>
            </div>
            {sessionId && (
              <Button onClick={handleNewAssessment} variant="outline">
                <Send className="w-4 h-4 mr-2" />
                Nova Avaliação
              </Button>
            )}
          </div>
        </div>

        {/* Error Alert */}
        {error && (
          <Alert variant="destructive" className="mb-6">
            <AlertCircle className="h-4 w-4" />
            <AlertTitle>Erro</AlertTitle>
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        )}

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Seção 1: Entrada de Dados do Paciente */}
          <Card className="shadow-lg border-2 hover:shadow-xl transition-shadow duration-300">
            <CardHeader className="bg-gradient-to-r from-blue-500 to-blue-600 text-white rounded-t-lg">
              <div className="flex items-center gap-2">
                <User className="w-5 h-5" />
                <CardTitle>Dados do Paciente</CardTitle>
              </div>
              <CardDescription className="text-blue-100">
                Insira as informações clínicas para análise
              </CardDescription>
            </CardHeader>
            <CardContent className="pt-6 space-y-4">
              <div className="space-y-2">
                <Label htmlFor="patientId" className="flex items-center gap-2">
                  <User className="w-4 h-4" />
                  ID do Paciente
                </Label>
                <Input
                  id="patientId"
                  placeholder="Ex: PAT-001"
                  value={patientData.patientId}
                  onChange={(e) => handleInputChange('patientId', e.target.value)}
                  disabled={!!sessionId}
                  className="transition-all duration-200 focus:ring-2 focus:ring-blue-500"
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="symptoms" className="flex items-center gap-2">
                  <Activity className="w-4 h-4" />
                  Sintomas
                </Label>
                <Textarea
                  id="symptoms"
                  placeholder="Descreva os sintomas apresentados pelo paciente..."
                  value={patientData.symptoms}
                  onChange={(e) => handleInputChange('symptoms', e.target.value)}
                  disabled={!!sessionId}
                  rows={4}
                  className="transition-all duration-200 focus:ring-2 focus:ring-blue-500"
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="medicalHistory" className="flex items-center gap-2">
                  <FileText className="w-4 h-4" />
                  Histórico Médico
                </Label>
                <Textarea
                  id="medicalHistory"
                  placeholder="Histórico médico relevante..."
                  value={patientData.medicalHistory}
                  onChange={(e) => handleInputChange('medicalHistory', e.target.value)}
                  disabled={!!sessionId}
                  rows={3}
                  className="transition-all duration-200 focus:ring-2 focus:ring-blue-500"
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="medications" className="flex items-center gap-2">
                  <Pill className="w-4 h-4" />
                  Medicações Atuais
                </Label>
                <Textarea
                  id="medications"
                  placeholder="Liste as medicações em uso (separadas por vírgula ou linha)..."
                  value={patientData.currentMedications}
                  onChange={(e) => handleInputChange('currentMedications', e.target.value)}
                  disabled={!!sessionId}
                  rows={2}
                  className="transition-all duration-200 focus:ring-2 focus:ring-blue-500"
                />
              </div>

              <Button 
                onClick={handleSubmitAssessment}
                disabled={isLoading || !!sessionId || !patientData.patientId || !patientData.symptoms}
                className="w-full bg-blue-600 hover:bg-blue-700 transition-all duration-200 transform hover:scale-105"
              >
                {isLoading ? (
                  <>
                    <Clock className="w-4 h-4 mr-2 animate-spin" />
                    Processando...
                  </>
                ) : (
                  <>
                    <Send className="w-4 h-4 mr-2" />
                    Enviar para Análise
                  </>
                )}
              </Button>

              {/* Session Info */}
              {sessionId && (
                <div className="mt-4 p-4 bg-blue-50 rounded-lg border border-blue-200">
                  <p className="text-xs font-semibold text-gray-600 mb-1">Session ID:</p>
                  <p className="text-xs text-gray-700 font-mono break-all">{sessionId}</p>
                </div>
              )}
            </CardContent>
          </Card>

          {/* Seção 2: Resultado da Análise */}
          <Card className="shadow-lg border-2 hover:shadow-xl transition-shadow duration-300">
            <CardHeader className="bg-gradient-to-r from-purple-500 to-purple-600 text-white rounded-t-lg">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Activity className="w-5 h-5" />
                  <CardTitle>Resultado da Análise</CardTitle>
                </div>
                {assessment && (
                  <Button 
                    onClick={handleRefreshStatus} 
                    variant="ghost" 
                    size="sm"
                    className="text-white hover:bg-purple-600"
                    disabled={isLoading}
                  >
                    <RefreshCw className={`w-4 h-4 ${isLoading ? 'animate-spin' : ''}`} />
                  </Button>
                )}
              </div>
              <CardDescription className="text-purple-100">
                Avaliação automática dos 4 agentes especializados
              </CardDescription>
            </CardHeader>
            <CardContent className="pt-6">
              {!assessment ? (
                <div className="flex flex-col items-center justify-center py-12 text-gray-400">
                  <Stethoscope className="w-16 h-16 mb-4 opacity-50" />
                  <p className="text-center">Aguardando envio dos dados do paciente para análise...</p>
                </div>
              ) : (
                <div className="space-y-4">
                  {/* Status Badge */}
                  <div className="flex items-center justify-between p-4 bg-gray-50 rounded-lg border border-gray-200">
                    <span className="font-semibold text-gray-700">Status:</span>
                    {getStatusBadge(assessment.status)}
                  </div>

                  {/* Message */}
                  {assessment.message && (
                    <Alert>
                      <AlertCircle className="h-4 w-4" />
                      <AlertDescription>{assessment.message}</AlertDescription>
                    </Alert>
                  )}

                  {/* Reprocessing Info */}
                  {assessment.data?.reprocessingCount > 0 && (
                    <Alert className="border-blue-500 bg-blue-50">
                      <History className="h-4 w-4 text-blue-600" />
                      <AlertTitle className="text-blue-900">Reprocessamento</AlertTitle>
                      <AlertDescription className="text-blue-800">
                        Iteração {assessment.data.reprocessingCount} de {assessment.data.maxReprocessingIterations || 3}
                        {assessment.data.physicianFeedback && (
                          <div className="mt-2 p-2 bg-white rounded border border-blue-200">
                            <p className="text-xs font-semibold mb-1">Feedback do Médico:</p>
                            <p className="text-xs">{assessment.data.physicianFeedback}</p>
                          </div>
                        )}
                      </AlertDescription>
                    </Alert>
                  )}

                  {/* Assessment Data */}
                  {assessment.data && (
                    <Tabs defaultValue="summary" className="w-full">
                      <TabsList className="grid w-full grid-cols-3">
                        <TabsTrigger value="summary">Resumo</TabsTrigger>
                        <TabsTrigger value="details">Detalhes</TabsTrigger>
                        <TabsTrigger value="fhir">FHIR</TabsTrigger>
                      </TabsList>
                      
                      <TabsContent value="summary" className="space-y-4 mt-4">
                        {/* Risk Level */}
                        {assessment.data.riskLevel && (
                          <div className="flex items-center justify-between p-4 bg-gradient-to-r from-red-50 to-red-100 rounded-lg border-2 border-red-200">
                            <div className="flex items-center gap-2">
                              <AlertCircle className="w-5 h-5 text-red-600" />
                              <span className="font-semibold text-gray-700">Nível de Risco:</span>
                            </div>
                            <Badge variant={getRiskVariant(assessment.data.riskLevel)} className="text-sm px-3 py-1">
                              {getRiskLevelText(assessment.data.riskLevel)}
                            </Badge>
                          </div>
                        )}

                        {/* Symptoms Summary */}
                        {assessment.data.symptomsSummary && (
                          <div className="p-4 bg-blue-50 rounded-lg border border-blue-200">
                            <p className="text-xs font-semibold text-gray-600 mb-2">Resumo dos Sintomas:</p>
                            <p className="text-sm text-gray-700">{assessment.data.symptomsSummary}</p>
                          </div>
                        )}

                        {/* Lab Exams */}
                        {assessment.data.recommendedLabExams && assessment.data.recommendedLabExams.length > 0 && (
                          <div className="space-y-2">
                            <div className="flex items-center gap-2 text-gray-700 font-semibold">
                              <FlaskConical className="w-4 h-4" />
                              <span>Exames Laboratoriais:</span>
                            </div>
                            <ul className="space-y-2">
                              {assessment.data.recommendedLabExams.map((exam, idx) => (
                                <li key={idx} className="flex items-start gap-2 p-2 bg-white rounded border border-gray-200 hover:border-purple-300 transition-colors">
                                  <CheckCircle2 className="w-4 h-4 text-green-500 mt-0.5 flex-shrink-0" />
                                  <span className="text-sm text-gray-700">{exam}</span>
                                </li>
                              ))}
                            </ul>
                          </div>
                        )}

                        {/* Imaging Exams */}
                        {assessment.data.recommendedImagingExams && assessment.data.recommendedImagingExams.length > 0 && (
                          <div className="space-y-2">
                            <div className="flex items-center gap-2 text-gray-700 font-semibold">
                              <Activity className="w-4 h-4" />
                              <span>Exames de Imagem:</span>
                            </div>
                            <ul className="space-y-2">
                              {assessment.data.recommendedImagingExams.map((exam, idx) => (
                                <li key={idx} className="flex items-start gap-2 p-2 bg-white rounded border border-gray-200 hover:border-purple-300 transition-colors">
                                  <CheckCircle2 className="w-4 h-4 text-green-500 mt-0.5 flex-shrink-0" />
                                  <span className="text-sm text-gray-700">{exam}</span>
                                </li>
                              ))}
                            </ul>
                          </div>
                        )}
                      </TabsContent>

                      <TabsContent value="details" className="mt-4 space-y-4">
                        {/* Drug Interactions */}
                        {assessment.data.drugInteractions && assessment.data.drugInteractions.length > 0 && (
                          <div className="p-4 bg-yellow-50 rounded-lg border border-yellow-200">
                            <p className="text-xs font-semibold text-gray-600 mb-2 flex items-center gap-2">
                              <Pill className="w-4 h-4" />
                              Interações Medicamentosas:
                            </p>
                            <ul className="space-y-1">
                              {assessment.data.drugInteractions.map((interaction, idx) => (
                                <li key={idx} className="text-sm text-gray-700">• {interaction}</li>
                              ))}
                            </ul>
                          </div>
                        )}

                        {/* Triage Recommendations */}
                        {assessment.data.triageRecommendations && (
                          <div className="p-4 bg-gray-50 rounded-lg border border-gray-200">
                            <p className="text-xs font-semibold text-gray-600 mb-2">Recomendações da Triagem:</p>
                            <p className="text-sm text-gray-700 whitespace-pre-wrap">{assessment.data.triageRecommendations}</p>
                          </div>
                        )}

                        {/* Pharmacist Recommendations */}
                        {assessment.data.pharmacistRecommendations && (
                          <div className="p-4 bg-gray-50 rounded-lg border border-gray-200">
                            <p className="text-xs font-semibold text-gray-600 mb-2">Recomendações Farmacêuticas:</p>
                            <p className="text-sm text-gray-700 whitespace-pre-wrap">{assessment.data.pharmacistRecommendations}</p>
                          </div>
                        )}

                        {/* Communication Text */}
                        {assessment.data.communicationText && (
                          <div className="p-4 bg-gray-50 rounded-lg border border-gray-200">
                            <p className="text-xs font-semibold text-gray-600 mb-2">Texto de Comunicação:</p>
                            <div className="prose prose-sm max-w-none text-gray-700 whitespace-pre-wrap">
                              {assessment.data.communicationText}
                            </div>
                          </div>
                        )}

                        {/* Assessment History */}
                        {assessment.data.assessmentHistory && assessment.data.assessmentHistory.length > 0 && (
                          <div className="p-4 bg-blue-50 rounded-lg border border-blue-200">
                            <p className="text-xs font-semibold text-gray-600 mb-2 flex items-center gap-2">
                              <History className="w-4 h-4" />
                              Histórico de Avaliações:
                            </p>
                            <ul className="space-y-1">
                              {assessment.data.assessmentHistory.map((entry, idx) => (
                                <li key={idx} className="text-xs text-gray-700">• {entry}</li>
                              ))}
                            </ul>
                          </div>
                        )}
                      </TabsContent>

                      <TabsContent value="fhir" className="mt-4">
                        <div className="p-4 bg-gray-900 rounded-lg overflow-x-auto">
                          <pre className="text-xs text-green-400">
                            {JSON.stringify(assessment.data.fhirDocument || assessment.data, null, 2)}
                          </pre>
                        </div>
                      </TabsContent>
                    </Tabs>
                  )}
                </div>
              )}
            </CardContent>
          </Card>
        </div>

        {/* Seção 3: Revisão Médica */}
        {assessment && assessment.status === 'AWAITING_APPROVAL' && (
          <Card className="mt-6 shadow-lg border-2 hover:shadow-xl transition-shadow duration-300">
            <CardHeader className="bg-gradient-to-r from-green-500 to-green-600 text-white rounded-t-lg">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <CheckCircle2 className="w-5 h-5" />
                  <CardTitle>Revisão Médica</CardTitle>
                </div>
                {getStatusBadge(assessment.status)}
              </div>
              <CardDescription className="text-green-100">
                Aprovação ou rejeição da documentação FHIR
                {assessment.data?.reprocessingCount > 0 && (
                  <span className="block mt-1">
                    (Iteração {assessment.data.reprocessingCount} - Você pode aprovar ou rejeitar novamente)
                  </span>
                )}
              </CardDescription>
            </CardHeader>
            <CardContent className="pt-6">
              <div className="space-y-4">
                <div className="space-y-2">
                  <Label htmlFor="comments" className="flex items-center gap-2">
                    <FileText className="w-4 h-4" />
                    Comentários do Médico
                    {assessment.data?.reprocessingCount > 0 && (
                      <span className="text-xs text-gray-500">(Forneça feedback específico se rejeitar)</span>
                    )}
                  </Label>
                  <Textarea
                    id="comments"
                    placeholder="Adicione seus comentários sobre a avaliação..."
                    value={approvalComments}
                    onChange={(e) => setApprovalComments(e.target.value)}
                    rows={4}
                    className="transition-all duration-200 focus:ring-2 focus:ring-green-500"
                  />
                </div>

                <div className="flex gap-3">
                  <Button
                    onClick={() => handleApproval('APPROVED')}
                    disabled={isLoading}
                    className="flex-1 bg-green-600 hover:bg-green-700 transition-all duration-200 transform hover:scale-105"
                  >
                    <CheckCircle2 className="w-4 h-4 mr-2" />
                    Aprovar Documentação
                  </Button>
                  <Button
                    onClick={() => handleApproval('REJECTED')}
                    disabled={isLoading}
                    variant="destructive"
                    className="flex-1 transition-all duration-200 transform hover:scale-105"
                  >
                    <XCircle className="w-4 h-4 mr-2" />
                    Rejeitar e Reprocessar
                  </Button>
                </div>

                {assessment.data?.reprocessingCount >= (assessment.data?.maxReprocessingIterations || 3) - 1 && (
                  <Alert variant="destructive">
                    <AlertCircle className="h-4 w-4" />
                    <AlertDescription>
                      Atenção: Esta é a última iteração permitida. Após rejeitar, a avaliação será marcada como rejeitada final.
                    </AlertDescription>
                  </Alert>
                )}
              </div>
            </CardContent>
          </Card>
        )}

        {/* Seção 4: Resultado Final */}
        {assessment && (assessment.status === 'COMPLETED' || assessment.status === 'REJECTED') && (
          <Card className="mt-6 shadow-lg border-2">
            <CardHeader className={`${assessment.status === 'COMPLETED' ? 'bg-gradient-to-r from-green-500 to-green-600' : 'bg-gradient-to-r from-red-500 to-red-600'} text-white rounded-t-lg`}>
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  {assessment.status === 'COMPLETED' ? <CheckCircle2 className="w-5 h-5" /> : <XCircle className="w-5 h-5" />}
                  <CardTitle>Resultado Final</CardTitle>
                </div>
                {getStatusBadge(assessment.status)}
              </div>
            </CardHeader>
            <CardContent className="pt-6">
              <div className="p-4 bg-gray-50 rounded-lg border border-gray-200">
                <p className="text-sm font-semibold text-gray-700 mb-2">Informações da Revisão:</p>
                <div className="space-y-1 text-sm text-gray-600">
                  <p><strong>Status:</strong> {assessment.status === 'COMPLETED' ? 'Aprovado' : 'Rejeitado'}</p>
                  {assessment.data?.reprocessingCount > 0 && (
                    <p><strong>Iterações de Reprocessamento:</strong> {assessment.data.reprocessingCount}</p>
                  )}
                  <p><strong>Mensagem:</strong> {assessment.message}</p>
                </div>
              </div>
            </CardContent>
          </Card>
        )}
      </div>
    </div>
  )
}

export default App

