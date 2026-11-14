import { api } from './api.js';
import {
    atualizarGraficoPeriodo,
    criarElemento,
    criarGraficoPeriodo,
    renderizarBarras,
    renderizarChamados,
    renderizarIndicadores,
    renderizarTabelaSerie,
} from './componentes.js';

const INTERVALO_ATUALIZACAO_MS = 60_000;
const DIAS_PERIODO_PADRAO = 90;
const formatoHorario = new Intl.DateTimeFormat('pt-BR', { timeStyle: 'short' });

const porId = (id) => document.getElementById(id);

const ui = {
    filtros: porId('filtros'),
    limparFiltros: porId('limpar-filtros'),
    alerta: porId('alerta'),
    indicadores: porId('indicadores'),
    agrupamento: porId('agrupamento'),
    tabelaSerie: porId('tabela-periodo'),
    status: porId('lista-status'),
    tipos: porId('ranking-tipos'),
    chamados: porId('tabela-chamados'),
    resumoPaginacao: porId('resumo-paginacao'),
    paginaAnterior: porId('pagina-anterior'),
    paginaSeguinte: porId('pagina-seguinte'),
    atualizar: porId('botao-atualizar'),
    ultimaAtualizacao: porId('ultima-atualizacao'),
    dialogo: porId('dialogo-status'),
    formStatus: porId('form-status'),
    protocoloDialogo: porId('dialogo-protocolo'),
    erroStatus: porId('erro-status'),
    cancelarStatus: porId('cancelar-status'),
    listaTecnicos: porId('lista-tecnicos'),
};

const estado = {
    filtros: {},
    agrupamento: 'SEMANA',
    pagina: 0,
    chamados: [],
    carregando: false,
};

let grafico;

function dataIso(data) {
    const local = new Date(data.getTime() - data.getTimezoneOffset() * 60_000);
    return local.toISOString().slice(0, 10);
}

function aplicarPeriodoPadrao() {
    const hoje = new Date();
    const inicio = new Date(hoje);
    inicio.setDate(hoje.getDate() - (DIAS_PERIODO_PADRAO - 1));
    ui.filtros.elements.dataInicio.value = dataIso(inicio);
    ui.filtros.elements.dataFim.value = dataIso(hoje);
}

function preencherSelect(select, opcoes, { manterPrimeira = true } = {}) {
    const primeira = manterPrimeira ? [select.options[0]] : [];
    select.replaceChildren(...primeira, ...opcoes.map(({ valor, descricao }) =>
        criarElemento('option', { value: valor, texto: descricao })));
}

function preencherOpcoes(opcoes) {
    const campos = ui.filtros.elements;
    const tecnicos = opcoes.tecnicos.map((nome) => ({ valor: nome, descricao: nome }));

    preencherSelect(campos.status, opcoes.status);
    preencherSelect(campos.tipo, opcoes.tipos);
    preencherSelect(campos.prioridade, opcoes.prioridades);
    preencherSelect(campos.tecnico, tecnicos);
    preencherSelect(ui.agrupamento, opcoes.agrupamentos, { manterPrimeira: false });
    ui.agrupamento.value = estado.agrupamento;

    preencherSelect(ui.formStatus.elements.status,
        opcoes.status.filter(({ valor }) => valor !== 'ABERTO'), { manterPrimeira: false });
    ui.listaTecnicos.replaceChildren(...opcoes.tecnicos.map((nome) => criarElemento('option', { value: nome })));
}

function mostrarAlerta(mensagem) {
    ui.alerta.textContent = mensagem ?? '';
    ui.alerta.hidden = !mensagem;
}

async function executar(acao) {
    try {
        await acao();
        mostrarAlerta();
    } catch (erro) {
        mostrarAlerta(erro.message);
    }
}

async function carregarSerie() {
    const serie = await api.serie(estado.filtros, estado.agrupamento);
    atualizarGraficoPeriodo(grafico, serie);
    renderizarTabelaSerie(ui.tabelaSerie, serie);
}

async function carregarChamados() {
    const pagina = await api.chamados(estado.filtros, estado.pagina);
    estado.chamados = pagina.conteudo;
    renderizarChamados(ui.chamados, pagina.conteudo);

    ui.resumoPaginacao.textContent = pagina.totalElementos
        ? `Página ${pagina.pagina + 1} de ${pagina.totalPaginas} · ${pagina.totalElementos} chamados`
        : '';
    ui.paginaAnterior.disabled = pagina.pagina === 0;
    ui.paginaSeguinte.disabled = pagina.pagina + 1 >= pagina.totalPaginas;
}

async function carregarPainel() {
    if (estado.carregando) {
        return;
    }
    estado.carregando = true;
    ui.atualizar.disabled = true;

    const { filtros } = estado;
    const resultados = await Promise.allSettled([
        api.indicadores(filtros).then((dados) => renderizarIndicadores(ui.indicadores, dados)),
        carregarSerie(),
        api.status(filtros).then((itens) => renderizarBarras(ui.status, itens.map((item) => ({
            rotulo: item.descricao,
            quantidade: item.quantidade,
            percentual: item.percentual,
        })))),
        api.tiposMaisAtendidos(filtros).then((itens) => renderizarBarras(ui.tipos, itens.map((item) => ({
            rotulo: `${item.posicao}º ${item.descricao}`,
            quantidade: item.quantidade,
            percentual: item.percentual,
        })))),
        carregarChamados(),
    ]);

    const falha = resultados.find((resultado) => resultado.status === 'rejected');
    mostrarAlerta(falha?.reason?.message);
    ui.ultimaAtualizacao.textContent = `Atualizado às ${formatoHorario.format(new Date())}`;
    ui.atualizar.disabled = false;
    estado.carregando = false;
}

function aplicarFiltros() {
    estado.filtros = Object.fromEntries(new FormData(ui.filtros));
    estado.pagina = 0;
    carregarPainel();
}

function mudarPagina(deslocamento) {
    estado.pagina += deslocamento;
    executar(carregarChamados);
}

function abrirDialogoStatus(protocolo) {
    const chamado = estado.chamados.find((item) => item.protocolo === protocolo);
    if (!chamado) {
        return;
    }
    const campos = ui.formStatus.elements;

    ui.formStatus.reset();
    ui.formStatus.dataset.protocolo = protocolo;
    ui.protocoloDialogo.textContent = protocolo;
    campos.status.value = chamado.status === 'ABERTO' ? 'EM_ANDAMENTO' : 'RESOLVIDO';
    campos.tecnico.value = chamado.tecnicoResponsavel ?? '';
    campos.consideracoes.value = chamado.consideracoesTecnicas ?? '';
    ui.erroStatus.hidden = true;
    ui.dialogo.showModal();
}

async function salvarStatus(evento) {
    evento.preventDefault();
    const dados = Object.fromEntries(new FormData(ui.formStatus));
    const botao = evento.submitter;
    botao.disabled = true;

    try {
        await api.atualizarStatus(ui.formStatus.dataset.protocolo, {
            status: dados.status,
            tecnico: dados.tecnico.trim() || null,
            consideracoes: dados.consideracoes.trim() || null,
        });
        ui.dialogo.close();
        await carregarPainel();
    } catch (erro) {
        ui.erroStatus.textContent = erro.message;
        ui.erroStatus.hidden = false;
    } finally {
        botao.disabled = false;
    }
}

function registrarEventos() {
    ui.filtros.addEventListener('submit', (evento) => {
        evento.preventDefault();
        aplicarFiltros();
    });
    ui.limparFiltros.addEventListener('click', () => {
        ui.filtros.reset();
        aplicarPeriodoPadrao();
        aplicarFiltros();
    });
    ui.agrupamento.addEventListener('change', () => {
        estado.agrupamento = ui.agrupamento.value;
        executar(carregarSerie);
    });
    ui.paginaAnterior.addEventListener('click', () => mudarPagina(-1));
    ui.paginaSeguinte.addEventListener('click', () => mudarPagina(1));
    ui.atualizar.addEventListener('click', carregarPainel);
    ui.chamados.addEventListener('click', (evento) => {
        const botao = evento.target.closest('button[data-protocolo]');
        if (botao) {
            abrirDialogoStatus(botao.dataset.protocolo);
        }
    });
    ui.formStatus.addEventListener('submit', salvarStatus);
    ui.cancelarStatus.addEventListener('click', () => ui.dialogo.close());
}

async function iniciar() {
    grafico = criarGraficoPeriodo(porId('grafico-periodo'));
    aplicarPeriodoPadrao();
    estado.filtros = Object.fromEntries(new FormData(ui.filtros));
    registrarEventos();

    await executar(async () => preencherOpcoes(await api.opcoesFiltro()));
    await carregarPainel();
    setInterval(carregarPainel, INTERVALO_ATUALIZACAO_MS);
}

iniciar();