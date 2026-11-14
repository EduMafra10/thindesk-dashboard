# Thindesk · Dashboard Administrativo de Chamados

Painel administrativo do sistema de chamados técnicos **Thindesk**, com API REST própria integrada ao MongoDB Atlas.

**Atividade Prática M2 · Dashboard Administrativo (individual)** · Eduardo Mafra

## Entregáveis

| Item | Onde está |
|---|---|
| Documentação completa (requisitos, endpoints, evidências de testes) | [`docs/DOCUMENTACAO.md`](docs/DOCUMENTACAO.md) |
| Vídeo de apresentação | [`docs/video/apresentacao.mp4`](docs/video/apresentacao.mp4) |
| Requisições de teste da API | [`docs/requisicoes.http`](docs/requisicoes.http) e [`docs/dashboard.http`](docs/dashboard.http) |
| Código-fonte | [`src/`](src/) |

## Funcionalidades

- **Indicadores gerais**: total, abertos, em andamento, aguardando usuário, resolvidos, cancelados, taxa de resolução, tempo médio de resolução e pendências de alta prioridade
- **Gráfico por período**: chamados abertos x resolvidos, agrupados por dia, semana ou mês
- **Situação dos chamados**: distribuição por status, com as considerações técnicas na tabela
- **Tipos mais atendidos**: ranking com quantidade e percentual
- **Filtros**: período, status, tipo, prioridade e técnico, aplicados a todo o painel
- **Atualização de dados**: atualização automática a cada 60 segundos e mudança de status pela tela

## Tecnologias

Java 21 · Spring Boot 4.1 · Spring Data MongoDB · Thymeleaf · Chart.js · MongoDB Atlas · JUnit 5 · Mockito

## Como executar

1. Copie `.env.example` para `.env` e preencha a string de conexão do MongoDB Atlas.
2. Execute `./mvnw spring-boot:run` (no Windows, `.\mvnw.cmd spring-boot:run`).
3. Acesse **http://localhost:8080**.

Testes: `./mvnw test`

## Endpoints

| Método | Endpoint | Descrição |
|---|---|---|
| GET | `/api/chamados` | Lista chamados com filtros e paginação |
| GET | `/api/chamados/{protocolo}` | Busca um chamado |
| POST | `/api/chamados` | Abre um chamado |
| PATCH | `/api/chamados/{protocolo}/status` | Atualiza o status |
| GET | `/api/dashboard/indicadores` | Indicadores gerais |
| GET | `/api/dashboard/atendimentos-por-periodo` | Série de abertos e resolvidos |
| GET | `/api/dashboard/status` | Situação dos chamados |
| GET | `/api/dashboard/tipos-mais-atendidos` | Ranking de tipos |
| GET | `/api/dashboard/opcoes-filtro` | Opções para os filtros |

Detalhes, exemplos e erros possíveis em [`docs/DOCUMENTACAO.md`](docs/DOCUMENTACAO.md#8-documentação-da-api).