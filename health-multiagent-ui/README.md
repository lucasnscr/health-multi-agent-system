# Health Multi-Agent System - Frontend UI

Interface web moderna e responsiva para o **Health Multi-Agent System**, construída com React, Vite, Tailwind CSS e shadcn/ui.

## 🎨 Características da UI

### Design Moderno
- ✅ Interface limpa e profissional
- ✅ Gradientes e animações suaves
- ✅ Componentes shadcn/ui de alta qualidade
- ✅ Responsivo para desktop e mobile
- ✅ Tema claro otimizado para ambiente clínico

### Funcionalidades Implementadas

#### 1. Entrada de Dados do Paciente
- Formulário intuitivo com validação
- Campos para ID, sintomas, histórico médico e medicações
- Desabilitação automática após submissão
- Exibição do Session ID

#### 2. Visualização de Resultados
- **3 abas organizadas:**
  - **Resumo:** Nível de risco, sintomas e exames recomendados
  - **Detalhes:** Interações medicamentosas, recomendações e histórico
  - **FHIR:** Documentação FHIR em JSON formatado

- **Badges de status dinâmicos:**
  - 🟢 Aprovado (COMPLETED)
  - 🔴 Rejeitado (REJECTED)
  - 🟡 Aguardando Aprovação (AWAITING_APPROVAL)
  - ⚪ Processando (PROCESSING)
  - 🔵 Reprocessando (REPROCESSING)

#### 3. Fluxo de Reprocessamento
- **Alert especial** quando em reprocessamento
- **Exibição do feedback** do médico anterior
- **Contador de iterações** (ex: "Iteração 1 de 3")
- **Histórico completo** de avaliações anteriores
- **Aviso** na última iteração permitida

#### 4. Revisão Médica (HITL)
- Área de comentários do médico
- Botões de aprovação/rejeição
- Feedback específico para reprocessamento
- Desabilitação após decisão final

#### 5. Polling Automático
- Atualização automática durante processamento
- Refresh manual disponível
- Timeout de 2 minutos

## 🚀 Instalação e Execução

### Pré-requisitos
- Node.js 18+ ou 22+
- pnpm (recomendado) ou npm
- Backend Spring Boot rodando em `http://localhost:8080`

### Instalação

```bash
# Instalar dependências
pnpm install
# ou
npm install
```

### Configuração

Edite o arquivo `.env` se necessário:

```env
# Backend API Base URL
VITE_API_BASE_URL=http://localhost:8080/api/health-assessment

# Polling interval in milliseconds
VITE_POLLING_INTERVAL=2000
```

### Executar em Desenvolvimento

```bash
pnpm dev
# ou
npm run dev
```

A aplicação estará disponível em: `http://localhost:5173`

### Build para Produção

```bash
pnpm build
# ou
npm run build
```

Os arquivos otimizados estarão em `dist/`

## 📁 Estrutura do Projeto

```
health-multiagent-ui/
├── src/
│   ├── components/
│   │   └── ui/              # Componentes shadcn/ui
│   ├── services/
│   │   └── api.js           # Serviço de comunicação com backend
│   ├── App.jsx              # Componente principal
│   ├── App.css              # Estilos customizados
│   └── main.jsx             # Entry point
├── .env                     # Configuração de ambiente
├── package.json             # Dependências
└── README.md                # Este arquivo
```

## 🔌 Integração com Backend

### Endpoints Utilizados

#### 1. Submeter Sintomas
```
POST /api/health-assessment/symptoms
```

#### 2. Consultar Status
```
GET /api/health-assessment/status/{sessionId}
```

#### 3. Aprovar/Rejeitar
```
POST /api/health-assessment/approve/{sessionId}
```

## 🎯 Fluxo de Uso

### Fluxo Normal (Aprovação Direta)

1. Médico preenche dados do paciente
2. Clica em "Enviar para Análise"
3. Sistema processa (4 agentes executam)
4. Resultados aparecem
5. Médico revisa e aprova
6. Status muda para "Aprovado"

### Fluxo com Reprocessamento

1. Médico preenche dados
2. Sistema processa
3. Médico não concorda
4. Médico escreve feedback específico
5. Clica em "Rejeitar e Reprocessar"
6. Sistema reprocessa incorporando feedback
7. Resultados atualizados aparecem
8. Médico pode aprovar ou rejeitar novamente (até 3 iterações)

## 📚 Tecnologias Utilizadas

- **React 19.1** - Framework UI
- **Vite 6.3** - Build tool
- **Tailwind CSS 4.1** - Estilização
- **shadcn/ui** - Componentes UI
- **Lucide React** - Ícones

