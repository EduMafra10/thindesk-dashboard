# Thindesk · Dashboard Administrativo de Chamados

**Atividade Prática M2 · Dashboard Administrativo (individual)**
**Aluno:** Eduardo Mafra

---

## Sumário

1. [Visão geral](#1-visão-geral)
2. [Tecnologias](#2-tecnologias)
3. [Arquitetura](#3-arquitetura)
4. [Requisitos funcionais](#4-requisitos-funcionais)
5. [Requisitos não funcionais](#5-requisitos-não-funcionais)
6. [Regras de negócio](#6-regras-de-negócio)
7. [Modelagem de dados](#7-modelagem-de-dados)
8. [Documentação da API](#8-documentação-da-api)
9. [Evidências de testes](#9-evidências-de-testes)
10. [Dashboard administrativo](#10-dashboard-administrativo)
11. [Como executar](#11-como-executar)
12. [Versionamento com Gitflow](#12-versionamento-com-gitflow)
13. [Principais decisões de análise e projeto](#13-principais-decisões-de-análise-e-projeto)

---

## 1. Visão geral

O **Thindesk** é um sistema de chamados técnicos. Este projeto entrega o **Dashboard Administrativo** do sistema e a **API REST** que o alimenta.

O painel permite que o responsável pelos chamados:

- acompanhe o andamento da operação por meio de indicadores gerais;
- analise o volume de chamados abertos e resolvidos ao longo do tempo;
- visualize a situação (status) dos chamados e as considerações técnicas registradas;
- identifique os tipos de chamado mais atendidos;
- filtre todas as informações por período, status, tipo, prioridade e técnico;
- atualize o status de um chamado diretamente pela tela.

---

## 2. Tecnologias

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot 4.1 (Spring Web MVC, Spring Data MongoDB, Bean Validation) |
| Interface | Thymeleaf, HTML, CSS e JavaScript (módulos ES) |
| Gráficos | Chart.js 4 |
| Banco de dados | MongoDB Atlas (cluster gerenciado na nuvem) |
| Testes | JUnit 5, Mockito, AssertJ, MockMvc |
| Build | Maven (Maven Wrapper) |
| Versionamento | Git + GitHub, fluxo Gitflow |

---

## 3. Arquitetura

```mermaid
flowchart LR
    Navegador["Dashboard<br/>(Thymeleaf + JavaScript)"] -- "fetch / JSON" --> Controllers
    subgraph API["API REST · Spring Boot"]
        Controllers["Controllers<br/>ChamadoController · DashboardController"] --> Services["Services<br/>ChamadoService · DashboardService"]
        Services --> Repositorio["Repository + MongoTemplate<br/>(consultas e agregações)"]
        Tratador["TratadorGlobalDeErros<br/>(Problem Details)"] -.-> Controllers
    end
    Repositorio --> Atlas[("MongoDB Atlas<br/>coleção chamados")]
```

O projeto é organizado em camadas:

| Pacote | Responsabilidade |
|---|---|
| `controller` | Recebe as requisições HTTP, valida os parâmetros e devolve as respostas |
| `service` | Orquestra os casos de uso e executa as consultas agregadas |
| `repository` | Acesso ao MongoDB e montagem dos filtros de consulta |
| `model` | Entidade `Chamado`, enums e **regras de negócio** do ciclo de vida do chamado |
| `dto` | Objetos de entrada e saída da API, para que a entidade nunca seja exposta diretamente |
| `exception` | Exceções de negócio e tratamento global de erros |
| `config` | Carga inicial de dados de demonstração |

O dashboard **não acessa o banco**: ele consome a mesma API REST documentada na seção 8, por meio de requisições `fetch` no navegador.

---

## 4. Requisitos funcionais

| Código | Requisito | Como foi atendido |
|---|---|---|
| **RF01** | Indicadores gerais de chamados técnicos | Endpoint `GET /api/dashboard/indicadores` e 8 cartões no painel: total, abertos, em andamento, aguardando usuário, resolvidos (com taxa de resolução), cancelados, tempo médio de resolução e pendências de alta prioridade |
| **RF02** | Análise de chamados atendidos por período (gráficos) | Endpoint `GET /api/dashboard/atendimentos-por-periodo` e gráfico de linhas com chamados **abertos x resolvidos**, agrupável por **dia, semana ou mês**, com tabela de dados alternativa |
| **RF03** | Situação dos chamados | Endpoint `GET /api/dashboard/status` e gráfico de barras com quantidade e percentual por status. A tabela de chamados exibe o status e as **considerações técnicas** de cada chamado |
| **RF04** | Tipos de chamados mais atendidos | Endpoint `GET /api/dashboard/tipos-mais-atendidos` e ranking com posição, quantidade e percentual |
| **RF05** | Filtros de consulta | Filtros por data inicial, data final, status, tipo, prioridade e técnico, aplicados a **todos** os blocos do painel. As opções vêm do endpoint `GET /api/dashboard/opcoes-filtro` |
| **RF06** | Integração com a API | Todo o painel é carregado a partir da API REST via `fetch`. A API persiste e consulta os dados no MongoDB Atlas |
| **RF07** | Atualização e tratamento de dados | Atualização automática a cada 60 segundos e manual pelo botão "Atualizar agora"; abertura de chamados (`POST`) e mudança de status (`PATCH`) com validação; erros padronizados exibidos na tela |

---

## 5. Requisitos não funcionais

| Código | Categoria | Requisito |
|---|---|---|
| **RNF01** | Desempenho | Contagens e médias são calculadas no próprio MongoDB (aggregation pipeline). A coleção possui índices em `protocolo` (único), `tipo`, `tecnicoResponsavel`, `dataAbertura` e um índice composto `status + dataAbertura`. A listagem é paginada, com no máximo 100 itens por página |
| **RNF02** | Segurança | Credenciais do banco ficam no arquivo `.env`, fora do repositório (`.gitignore`). O usuário do banco tem permissão apenas no banco `thindesk` e o acesso ao Atlas é restrito por IP. Todas as entradas são validadas (Bean Validation e padrão `TD-000000` para protocolos). As consultas são montadas com `Criteria` tipado, sem concatenar texto do usuário. Respostas de erro não expõem stack trace. No painel, os dados são inseridos com `textContent`, o que impede XSS |
| **RNF03** | Padronização | API REST com JSON, códigos HTTP semânticos (200, 201, 400, 404, 422, 500) e erros no formato **Problem Details (RFC 9457)** |
| **RNF04** | Usabilidade e acessibilidade | Layout responsivo; filtros em uma única barra; tooltip nos gráficos; tabela alternativa para os dados do gráfico; status sempre identificado por texto, e não apenas por cor; foco visível nos controles |
| **RNF05** | Atualização dos dados | O painel se atualiza automaticamente a cada 60 segundos e informa o horário da última atualização |
| **RNF06** | Manutenibilidade | Separação em camadas, regras de negócio concentradas na entidade de domínio e 25 testes automatizados |
| **RNF07** | Portabilidade | Execução com Java 21 e Maven Wrapper, sem instalação de Maven. Banco na nuvem (MongoDB Atlas) |
| **RNF08** | Versionamento | Código versionado no GitHub seguindo o fluxo Gitflow, com integração por Pull Requests |

---

## 6. Regras de negócio

| Código | Regra |
|---|---|
| **RN01** | Todo chamado é criado com status **Aberto** e recebe um protocolo sequencial no formato `TD-000000` |
| **RN02** | Para iniciar o atendimento é obrigatório informar o **técnico responsável** |
| **RN03** | Só é possível resolver um chamado que esteja **em atendimento** (Em andamento ou Aguardando usuário) |
| **RN04** | Para resolver um chamado é obrigatório registrar as **considerações técnicas** |
| **RN05** | Chamados **Resolvidos** ou **Cancelados** são finalizados e não podem mais ser alterados |
| **RN06** | Um chamado não pode retornar ao status **Aberto** |
| **RN07** | Ao retomar um atendimento, a data de início original é preservada |
| **RN08** | Nos filtros, a data inicial não pode ser posterior à data final; a data final inclui o dia inteiro |
| **RN09** | A série por período é limitada a 400 pontos. Para períodos longos, deve-se usar o agrupamento semanal ou mensal |
| **RN10** | "Tipos mais atendidos" considera apenas chamados cujo atendimento já foi iniciado |
| **RN11** | Taxa de resolução = resolvidos ÷ total. Tempo médio de resolução = média de (data de conclusão − data de abertura) dos chamados resolvidos |

### Ciclo de vida do chamado

```mermaid
stateDiagram-v2
    [*] --> ABERTO
    ABERTO --> EM_ANDAMENTO: iniciar atendimento (técnico obrigatório)
    ABERTO --> CANCELADO: cancelar
    EM_ANDAMENTO --> AGUARDANDO_USUARIO: aguardar retorno
    AGUARDANDO_USUARIO --> EM_ANDAMENTO: retomar atendimento
    EM_ANDAMENTO --> RESOLVIDO: resolver (considerações obrigatórias)
    AGUARDANDO_USUARIO --> RESOLVIDO: resolver (considerações obrigatórias)
    EM_ANDAMENTO --> CANCELADO: cancelar
    AGUARDANDO_USUARIO --> CANCELADO: cancelar
    RESOLVIDO --> [*]
    CANCELADO --> [*]
```

---

## 7. Modelagem de dados

Banco: **`thindesk`** · Coleção: **`chamados`**

Foi escolhido o modelo de **documento único por chamado**: todas as informações consultadas pelo painel ficam no mesmo documento, o que evita junções e permite agregações diretas.

| Campo | Tipo | Descrição |
|---|---|---|
| `_id` | ObjectId | Identificador interno do MongoDB |
| `protocolo` | String | Identificador público do chamado, único (`TD-000000`) |
| `titulo` | String | Resumo do problema (até 120 caracteres) |
| `descricao` | String | Descrição detalhada (até 2000 caracteres) |
| `tipo` | Enum | `HARDWARE`, `SOFTWARE`, `REDE`, `ACESSO`, `IMPRESSORA`, `EMAIL`, `SISTEMA_INTERNO` |
| `prioridade` | Enum | `BAIXA`, `MEDIA`, `ALTA`, `CRITICA` |
| `status` | Enum | `ABERTO`, `EM_ANDAMENTO`, `AGUARDANDO_USUARIO`, `RESOLVIDO`, `CANCELADO` |
| `solicitante` | String | Quem abriu o chamado |
| `setor` | String | Setor do solicitante |
| `tecnicoResponsavel` | String | Técnico que assumiu o atendimento |
| `consideracoesTecnicas` | String | Registro técnico da solução ou do motivo do cancelamento |
| `dataAbertura` | Date | Data e hora de abertura |
| `dataInicioAtendimento` | Date | Data e hora do primeiro atendimento |
| `dataConclusao` | Date | Data e hora de resolução ou cancelamento |
| `atualizadoEm` | Date | Data e hora da última alteração |

**Índices:** `protocolo` (único), `tipo`, `tecnicoResponsavel`, `dataAbertura` e `status_data_abertura` (`status` + `dataAbertura`).

Exemplo de documento:

```json
{
  "_id": { "$oid": "6735a1f2c9e77a3b1d0f4a21" },
  "protocolo": "TD-000014",
  "titulo": "Usuário bloqueado",
  "descricao": "Solicitação registrada pelo portal: usuário bloqueado.",
  "tipo": "ACESSO",
  "prioridade": "ALTA",
  "status": "RESOLVIDO",
  "solicitante": "Larissa Nunes",
  "setor": "Financeiro",
  "tecnicoResponsavel": "Beatriz Souza",
  "consideracoesTecnicas": "Senha redefinida e usuário orientado sobre a política de senhas.",
  "dataAbertura": { "$date": "2026-05-22T12:31:00Z" },
  "dataInicioAtendimento": { "$date": "2026-05-22T13:05:00Z" },
  "dataConclusao": { "$date": "2026-05-22T16:40:00Z" },
  "atualizadoEm": { "$date": "2026-05-22T16:40:00Z" }
}
```

Na primeira execução com a coleção vazia, a classe `CargaInicialDados` cadastra **320 chamados** distribuídos pelos últimos 180 dias, com tipos, prioridades e status variados, para demonstrar o painel. Ela é controlada pela propriedade `thindesk.carga-inicial.habilitada`.

---

## 8. Documentação da API

### 8.1 Convenções

- **URL base:** `http://localhost:8080/api`
- **Formato:** JSON (`application/json`) e erros em `application/problem+json`
- **Datas nos filtros:** `AAAA-MM-DD` (ex.: `2026-11-14`)
- **Datas nas respostas:** ISO 8601 (ex.: `2026-11-14T00:12:00`)
- **Enums:** enviados e recebidos pelo nome (ex.: `EM_ANDAMENTO`). As respostas também trazem a descrição legível (ex.: `"statusDescricao": "Em andamento"`)

### 8.2 Filtros comuns

Aceitos por `GET /api/chamados` e por todos os endpoints `GET /api/dashboard/*`, exceto `opcoes-filtro`. Todos são opcionais e podem ser combinados.

| Parâmetro | Tipo | Exemplo | Descrição |
|---|---|---|---|
| `status` | Enum | `RESOLVIDO` | Status do chamado |
| `tipo` | Enum | `ACESSO` | Tipo do chamado |
| `prioridade` | Enum | `ALTA` | Prioridade |
| `tecnico` | Texto | `Carlos Mendes` | Técnico responsável (nome exato) |
| `dataInicio` | Data | `2026-10-01` | Abertura a partir desta data |
| `dataFim` | Data | `2026-11-14` | Abertura até esta data (dia inteiro) |

### 8.3 Formato dos erros

Todos os erros seguem o padrão **Problem Details (RFC 9457)**:

```json
{
  "title": "Dados inválidos",
  "status": 400,
  "detail": "Um ou mais campos estão inválidos.",
  "instance": "/api/chamados",
  "timestamp": "2026-11-14T03:20:11.482Z",
  "erros": [
    { "campo": "titulo", "mensagem": "O título é obrigatório." },
    { "campo": "tipo", "mensagem": "O tipo é obrigatório." }
  ]
}
```

| Código | Título | Quando ocorre |
|---|---|---|
| **400** | Dados inválidos / Requisição inválida | Campo obrigatório ausente, valor fora do permitido, enum inexistente, protocolo fora do padrão `TD-000000`, paginação fora do limite ou JSON mal formatado |
| **404** | Recurso não encontrado | Protocolo inexistente |
| **422** | Regra de negócio violada | Alguma regra da seção 6 não foi atendida |
| **500** | Erro interno | Falha inesperada. O detalhe técnico vai apenas para o log do servidor |

---

### 8.4 Endpoints de chamados

#### `GET /api/chamados` · Listar chamados

Lista paginada, ordenada da abertura mais recente para a mais antiga.

| Parâmetro | Tipo | Padrão | Regras |
|---|---|---|---|
| filtros comuns | — | — | Seção 8.2 |
| `pagina` | inteiro | `0` | Mínimo 0 |
| `tamanho` | inteiro | `20` | Entre 1 e 100 |

**Requisição**
```http
GET /api/chamados?status=RESOLVIDO&tipo=ACESSO&pagina=0&tamanho=1
```

**Resposta `200 OK`**
```json
{
  "conteudo": [
    {
      "protocolo": "TD-000314",
      "titulo": "Usuário bloqueado",
      "descricao": "Solicitação registrada pelo portal: usuário bloqueado.",
      "tipo": "ACESSO",
      "tipoDescricao": "Acessos e senhas",
      "prioridade": "BAIXA",
      "prioridadeDescricao": "Baixa",
      "status": "RESOLVIDO",
      "statusDescricao": "Resolvido",
      "solicitante": "Ana Ribeiro",
      "setor": "Recursos Humanos",
      "tecnicoResponsavel": "Beatriz Souza",
      "consideracoesTecnicas": "Permissão concedida após aprovação do gestor.",
      "dataAbertura": "2026-11-11T15:39:00",
      "dataInicioAtendimento": "2026-11-11T16:32:00",
      "dataConclusao": "2026-11-12T16:51:00",
      "tempoAtendimentoMinutos": 1512
    }
  ],
  "pagina": 0,
  "tamanho": 1,
  "totalElementos": 61,
  "totalPaginas": 61
}
```

**Erros:** `400` (filtro ou paginação inválidos) · `422` (data inicial posterior à final)

---

#### `GET /api/chamados/{protocolo}` · Buscar chamado

| Parâmetro | Local | Regras |
|---|---|---|
| `protocolo` | caminho | Formato `TD-000000` |

**Requisição**
```http
GET /api/chamados/TD-000001
```

**Resposta `200 OK`:** um objeto chamado, no mesmo formato de um item de `conteudo` acima.

**Erros**
```json
{ "title": "Recurso não encontrado", "status": 404, "detail": "Chamado TD-999999 não encontrado.", "instance": "/api/chamados/TD-999999" }
```
`400` quando o protocolo não segue o padrão (ex.: `/api/chamados/abc`).

---

#### `POST /api/chamados` · Abrir chamado

| Campo | Tipo | Obrigatório | Regras |
|---|---|---|---|
| `titulo` | texto | sim | Até 120 caracteres |
| `descricao` | texto | sim | Até 2000 caracteres |
| `tipo` | enum | sim | Valores da seção 7 |
| `prioridade` | enum | sim | `BAIXA`, `MEDIA`, `ALTA`, `CRITICA` |
| `solicitante` | texto | sim | Até 100 caracteres |
| `setor` | texto | sim | Até 60 caracteres |

**Requisição**
```http
POST /api/chamados
Content-Type: application/json

{
  "titulo": "Notebook sem áudio",
  "descricao": "O notebook não reproduz som após a última atualização do sistema.",
  "tipo": "HARDWARE",
  "prioridade": "MEDIA",
  "solicitante": "Ana Ribeiro",
  "setor": "Comercial"
}
```

**Resposta `201 Created`**
```http
Location: http://localhost:8080/api/chamados/TD-000321
```
```json
{
  "protocolo": "TD-000321",
  "titulo": "Notebook sem áudio",
  "descricao": "O notebook não reproduz som após a última atualização do sistema.",
  "tipo": "HARDWARE",
  "tipoDescricao": "Hardware",
  "prioridade": "MEDIA",
  "prioridadeDescricao": "Média",
  "status": "ABERTO",
  "statusDescricao": "Aberto",
  "solicitante": "Ana Ribeiro",
  "setor": "Comercial",
  "tecnicoResponsavel": null,
  "consideracoesTecnicas": null,
  "dataAbertura": "2026-11-14T00:12:00",
  "dataInicioAtendimento": null,
  "dataConclusao": null,
  "tempoAtendimentoMinutos": null
}
```

**Erros:** `400` com a lista `erros` indicando cada campo inválido (exemplo na seção 8.3).

---

#### `PATCH /api/chamados/{protocolo}/status` · Atualizar status

| Campo | Tipo | Obrigatório | Regras |
|---|---|---|---|
| `status` | enum | sim | `EM_ANDAMENTO`, `AGUARDANDO_USUARIO`, `RESOLVIDO` ou `CANCELADO` |
| `tecnico` | texto | para `EM_ANDAMENTO` | Até 100 caracteres |
| `consideracoes` | texto | para `RESOLVIDO` | Até 2000 caracteres. Em `CANCELADO`, registra o motivo |

**Requisição: iniciar atendimento**
```http
PATCH /api/chamados/TD-000321/status
Content-Type: application/json

{ "status": "EM_ANDAMENTO", "tecnico": "Carlos Mendes" }
```

**Requisição: resolver**
```http
PATCH /api/chamados/TD-000321/status
Content-Type: application/json

{
  "status": "RESOLVIDO",
  "consideracoes": "Driver de áudio reinstalado e funcionamento validado com a usuária."
}
```

**Resposta `200 OK`:** o chamado atualizado, com `status`, `tecnicoResponsavel`, `consideracoesTecnicas`, `dataConclusao` e `tempoAtendimentoMinutos` preenchidos conforme a operação.

**Erros**

| Código | Situação | `detail` |
|---|---|---|
| 400 | Status inexistente ou JSON inválido | `O corpo da requisição está ausente ou em formato inválido.` |
| 404 | Protocolo inexistente | `Chamado TD-999999 não encontrado.` |
| 422 | Resolver sem iniciar o atendimento | `O chamado TD-000321 não está em atendimento.` |
| 422 | Resolver sem considerações | `Informe as considerações técnicas para resolver o chamado.` |
| 422 | Alterar chamado finalizado | `O chamado TD-000321 já foi finalizado.` |
| 422 | Voltar para Aberto | `Um chamado não pode retornar ao status Aberto.` |
| 422 | Iniciar sem técnico | `Informe o técnico responsável pelo atendimento.` |

---

### 8.5 Endpoints do dashboard

#### `GET /api/dashboard/indicadores` · Indicadores gerais (RF01)

**Parâmetros:** filtros comuns (seção 8.2).

**Requisição**
```http
GET /api/dashboard/indicadores
```

**Resposta `200 OK`**
```json
{
  "total": 321,
  "abertos": 7,
  "emAndamento": 13,
  "aguardandoUsuario": 6,
  "resolvidos": 271,
  "cancelados": 24,
  "taxaResolucao": 84.4,
  "tempoMedioResolucaoHoras": 39.3,
  "pendentesAltaPrioridade": 9
}
```

| Campo | Descrição |
|---|---|
| `taxaResolucao` | Percentual de resolvidos sobre o total |
| `tempoMedioResolucaoHoras` | Média, em horas, entre abertura e conclusão dos resolvidos. `null` se não houver resolvidos |
| `pendentesAltaPrioridade` | Chamados de prioridade alta ou crítica ainda não concluídos |

**Erros:** `400` (filtro inválido) · `422` (período invertido)

---

#### `GET /api/dashboard/atendimentos-por-periodo` · Série por período (RF02)

| Parâmetro | Tipo | Padrão | Regras |
|---|---|---|---|
| filtros comuns | — | — | Seção 8.2 |
| `agrupamento` | enum | `SEMANA` | `DIA`, `SEMANA` (iniciada na segunda-feira) ou `MES` |

Os períodos sem nenhum chamado são retornados com zero, para que o gráfico não tenha lacunas.

**Requisição**
```http
GET /api/dashboard/atendimentos-por-periodo?agrupamento=SEMANA&dataInicio=2026-11-01&dataFim=2026-11-14
```

**Resposta `200 OK`** (valores ilustrativos)
```json
[
  { "inicio": "2026-10-26", "rotulo": "26/10", "abertos": 1, "resolvidos": 2 },
  { "inicio": "2026-11-02", "rotulo": "02/11", "abertos": 14, "resolvidos": 8 },
  { "inicio": "2026-11-09", "rotulo": "09/11", "abertos": 12, "resolvidos": 6 }
]
```

**Erros**

| Código | Situação |
|---|---|
| 400 | `agrupamento` inválido (ex.: `ANO`) |
| 422 | Período invertido ou longo demais para o agrupamento (mais de 400 pontos) |

---

#### `GET /api/dashboard/status` · Situação dos chamados (RF03)

**Parâmetros:** filtros comuns (seção 8.2).

**Resposta `200 OK`**
```json
[
  { "status": "ABERTO", "descricao": "Aberto", "quantidade": 7, "percentual": 2.2 },
  { "status": "EM_ANDAMENTO", "descricao": "Em andamento", "quantidade": 13, "percentual": 4.0 },
  { "status": "AGUARDANDO_USUARIO", "descricao": "Aguardando usuário", "quantidade": 6, "percentual": 1.9 },
  { "status": "RESOLVIDO", "descricao": "Resolvido", "quantidade": 271, "percentual": 84.4 },
  { "status": "CANCELADO", "descricao": "Cancelado", "quantidade": 24, "percentual": 7.5 }
]
```

Todos os status são sempre retornados, inclusive com quantidade zero.

---

#### `GET /api/dashboard/tipos-mais-atendidos` · Ranking de tipos (RF04)

| Parâmetro | Tipo | Padrão | Regras |
|---|---|---|---|
| filtros comuns | — | — | Seção 8.2 |
| `limite` | inteiro | `5` | Entre 1 e 7 |

**Requisição**
```http
GET /api/dashboard/tipos-mais-atendidos?limite=3
```

**Resposta `200 OK`**
```json
[
  { "posicao": 1, "tipo": "SOFTWARE", "descricao": "Software", "quantidade": 77, "percentual": 24.5 },
  { "posicao": 2, "tipo": "ACESSO", "descricao": "Acessos e senhas", "quantidade": 75, "percentual": 23.9 },
  { "posicao": 3, "tipo": "REDE", "descricao": "Rede e conectividade", "quantidade": 49, "percentual": 15.6 }
]
```

O percentual é calculado sobre o total de chamados com atendimento iniciado.

**Erros:** `400` (`limite` fora de 1 a 7) · `422` (período invertido)

---

#### `GET /api/dashboard/opcoes-filtro` · Opções dos filtros (RF05)

Sem parâmetros. Retorna os valores para preencher os filtros da tela, incluindo os técnicos cadastrados.

**Resposta `200 OK`**
```json
{
  "status": [
    { "valor": "ABERTO", "descricao": "Aberto" },
    { "valor": "EM_ANDAMENTO", "descricao": "Em andamento" },
    { "valor": "AGUARDANDO_USUARIO", "descricao": "Aguardando usuário" },
    { "valor": "RESOLVIDO", "descricao": "Resolvido" },
    { "valor": "CANCELADO", "descricao": "Cancelado" }
  ],
  "tipos": [
    { "valor": "HARDWARE", "descricao": "Hardware" },
    { "valor": "SOFTWARE", "descricao": "Software" },
    { "valor": "REDE", "descricao": "Rede e conectividade" },
    { "valor": "ACESSO", "descricao": "Acessos e senhas" },
    { "valor": "IMPRESSORA", "descricao": "Impressoras" },
    { "valor": "EMAIL", "descricao": "E-mail" },
    { "valor": "SISTEMA_INTERNO", "descricao": "Sistemas internos" }
  ],
  "prioridades": [
    { "valor": "BAIXA", "descricao": "Baixa" },
    { "valor": "MEDIA", "descricao": "Média" },
    { "valor": "ALTA", "descricao": "Alta" },
    { "valor": "CRITICA", "descricao": "Crítica" }
  ],
  "agrupamentos": [
    { "valor": "DIA", "descricao": "Dia" },
    { "valor": "SEMANA", "descricao": "Semana" },
    { "valor": "MES", "descricao": "Mês" }
  ],
  "tecnicos": ["Beatriz Souza", "Carlos Mendes", "Diego Martins", "Juliana Prado", "Rafael Lima"]
}
```

---

## 9. Evidências de testes

### 9.1 Testes automatizados

Comando: `./mvnw test` (no Windows, `.\mvnw.cmd test`)

Resultado: **25 testes executados, 0 falhas, 0 erros.**

![Resultado da execução dos testes automatizados](evidencias/01-testes-automatizados.png)

![Testes no painel Testing do VS Code](evidencias/02-testes-vscode.png)

| Classe | Tipo | Cenários verificados |
|---|---|---|
| `ChamadoTest` | Unitário (domínio) | Chamado novo começa Aberto e sem técnico · Iniciar atendimento registra técnico e data · Não inicia sem técnico · Retomar preserva a data de início · Não resolve sem atendimento · Não resolve sem considerações · Resolver registra considerações e conclusão · Chamado finalizado não pode ser alterado |
| `ChamadoServiceTest` | Unitário (Mockito) | Gera o próximo protocolo da sequência · Primeiro protocolo é `TD-000001` · Protocolo inexistente lança exceção · Em andamento registra o técnico · Não permite voltar para Aberto |
| `ChamadoControllerTest` | Web (MockMvc) | `POST` retorna 201 com `Location` · `POST` inválido retorna 400 com a lista de erros · Status inexistente retorna 400 · Protocolo inexistente retorna 404 · Regra violada retorna 422 |
| `AgrupamentoTest` | Unitário | Dia mantém a data · Semana começa na segunda · Mês começa no dia 1 · Próximo mês · Rótulos no formato brasileiro |
| `ChamadoCriteriaTest` | Unitário | Rejeita período invertido · Data final inclui o dia inteiro |

### 9.2 Testes manuais da API

Executados com a extensão **REST Client** do VS Code, a partir dos arquivos [`requisicoes.http`](requisicoes.http) e [`dashboard.http`](dashboard.http), com a aplicação conectada ao MongoDB Atlas.

| # | Cenário | Requisição | Esperado | Resultado |
|---|---|---|---|---|
| 1 | Abrir chamado | `POST /api/chamados` | 201 + protocolo gerado | ✅ |
| 2 | Iniciar atendimento | `PATCH .../status` `EM_ANDAMENTO` | 200 | ✅ |
| 3 | Resolver sem considerações | `PATCH .../status` `RESOLVIDO` | 422 | ✅ |
| 4 | Resolver com considerações | `PATCH .../status` `RESOLVIDO` | 200 | ✅ |
| 5 | Alterar chamado finalizado | `PATCH .../status` `CANCELADO` | 422 | ✅ |
| 6 | Protocolo inexistente | `GET /api/chamados/TD-999999` | 404 | ✅ |
| 7 | Protocolo fora do padrão | `GET /api/chamados/abc` | 400 | ✅ |
| 8 | Período invertido | `GET /api/chamados?dataInicio=2026-11-10&dataFim=2026-11-01` | 422 | ✅ |
| 9 | Indicadores gerais | `GET /api/dashboard/indicadores` | 200 | ✅ |
| 10 | Ranking de tipos | `GET /api/dashboard/tipos-mais-atendidos` | 200 | ✅ |
| 11 | Agrupamento inválido | `GET .../atendimentos-por-periodo?agrupamento=ANO` | 400 | ✅ |

![Abertura de chamado](evidencias/03-post-criar-chamado.png)

![Resolver sem considerações técnicas retorna 422](evidencias/04-patch-resolver-sem-consideracoes-422.png)

![Chamado resolvido com considerações técnicas](evidencias/05-patch-resolver-200.png)

![Protocolo inexistente retorna 404](evidencias/06-get-protocolo-inexistente-404.png)

![Indicadores do dashboard](evidencias/07-dashboard-indicadores-api.png)

![Ranking de tipos mais atendidos](evidencias/08-dashboard-tipos-api.png)

### 9.3 Banco de dados

![Coleção chamados no MongoDB Atlas](evidencias/09-atlas-colecao.png)

---

## 10. Dashboard administrativo

Disponível em `http://localhost:8080` após iniciar a aplicação.

![Filtros e indicadores gerais](evidencias/10-painel-indicadores.png)

![Gráfico de chamados por período](evidencias/11-painel-grafico.png)

![Situação dos chamados e tipos mais atendidos](evidencias/12-painel-status-tipos.png)

![Tabela de chamados com considerações técnicas](evidencias/13-painel-tabela.png)

![Atualização de status com mensagem de erro da API](evidencias/14-painel-atualizar-status.png)

| Bloco | Requisito | Fonte dos dados |
|---|---|---|
| Barra de filtros | RF05 | `GET /api/dashboard/opcoes-filtro` |
| Cartões de indicadores | RF01 | `GET /api/dashboard/indicadores` |
| Gráfico por período + tabela de dados | RF02 | `GET /api/dashboard/atendimentos-por-periodo` |
| Situação dos chamados | RF03 | `GET /api/dashboard/status` |
| Tipos mais atendidos | RF04 | `GET /api/dashboard/tipos-mais-atendidos` |
| Tabela de chamados e atualização de status | RF03, RF07 | `GET /api/chamados` e `PATCH /api/chamados/{protocolo}/status` |
| Atualização automática (60 s) e botão "Atualizar agora" | RF07 | Todos os endpoints acima |

---

## 11. Como executar

### Pré-requisitos

- Java 21
- Um cluster no MongoDB Atlas (o plano gratuito é suficiente) com usuário de banco e IP liberado

### Passos

1. Clone o repositório:
   ```bash
   git clone https://github.com/EduMafra10/thindesk-dashboard.git
   cd thindesk-dashboard
   ```
2. Copie o arquivo `.env.example` para `.env` e preencha a string de conexão do Atlas:
   ```properties
   MONGODB_URI=mongodb+srv://usuario:senha@cluster.mongodb.net/?appName=cluster
   MONGODB_DATABASE=thindesk
   ```
3. Inicie a aplicação:
   ```bash
   ./mvnw spring-boot:run
   ```
   No Windows: `.\mvnw.cmd spring-boot:run`
4. Acesse o dashboard em **http://localhost:8080**. A API fica em **http://localhost:8080/api**.

Na primeira execução, a carga inicial cadastra os chamados de demonstração automaticamente.

### Executar os testes

```bash
./mvnw test
```

---

## 12. Versionamento com Gitflow

| Branch | Finalidade |
|---|---|
| `main` | Versão estável entregue (`v1.0.0`) |
| `develop` | Integração das funcionalidades concluídas |
| `feature/modelagem-dados` | Entidade `Chamado`, enums, repositório e carga inicial |
| `feature/api-chamados` | Endpoints de chamados, filtros, paginação e tratamento de erros |
| `feature/api-dashboard` | Endpoints de indicadores, série por período, status e ranking |
| `feature/dashboard-web` | Interface do dashboard em Thymeleaf |
| `feature/testes` | Testes automatizados |
| `feature/documentacao` | Esta documentação e o README |
| `release/1.0.0` | Preparação da versão entregue, mesclada em `main` e `develop` |

Cada `feature/*` foi criada a partir de `develop` e integrada por **Pull Request**.

---

## 13. Principais decisões de análise e projeto

| Decisão | Justificativa |
|---|---|
| **MongoDB com um documento por chamado** | Todos os dados que o painel consulta ficam juntos, sem junções. O formato flexível facilita evoluir o chamado com novos campos |
| **Agregações executadas no banco** | Contagens por status e por tipo e o tempo médio são calculados pelo MongoDB (aggregation pipeline), sem trazer todos os registros para a aplicação |
| **Série temporal montada na aplicação** | Permite preencher com zero os períodos sem chamados e padronizar os rótulos (dia, semana iniciada na segunda e mês) |
| **Regras de negócio na entidade `Chamado`** | O ciclo de vida fica protegido em um único lugar. Qualquer caminho que altere o chamado (API, carga inicial ou testes) passa pelas mesmas validações |
| **DTOs separados da entidade** | A API expõe apenas o necessário e pode evoluir sem alterar o modelo do banco |
| **Filtros reaproveitados (`ChamadoCriteria`)** | Os mesmos filtros valem para a listagem e para todos os indicadores, então os números do painel são sempre coerentes entre si |
| **Erros no padrão Problem Details** | Formato único e documentado para qualquer erro, que o painel exibe diretamente ao usuário |
| **Dashboard consumindo a API via `fetch`** | A interface fica desacoplada do servidor e usa exatamente o mesmo contrato documentado, o que comprova a integração (RF06) |
| **Protocolo legível (`TD-000000`)** | Identificador amigável para o usuário, separado do `_id` interno do MongoDB |
| **Testes unitários sem dependência de banco** | Executam em segundos e validam regras, serviços e respostas HTTP sem depender da internet |