const formatoNumero = new Intl.NumberFormat('pt-BR');
const formatoDecimal = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 1 });
const formatoDataHora = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' });
const STATUS_FINALIZADOS = new Set(['RESOLVIDO', 'CANCELADO']);

export function criarElemento(tag, atributos = {}, ...filhos) {
    const elemento = document.createElement(tag);
    Object.entries(atributos).forEach(([nome, valor]) => {
        if (nome === 'texto') {
            elemento.textContent = valor;
        } else {
            elemento.setAttribute(nome, valor);
        }
    });
    elemento.append(...filhos);
    return elemento;
}

function criarCartao(rotulo, valor, detalhe) {
    return criarElemento('article', { class: 'indicador' },
        criarElemento('p', { class: 'indicador-rotulo', texto: rotulo }),
        criarElemento('p', { class: 'indicador-valor', texto: valor }),
        criarElemento('p', { class: 'indicador-detalhe', texto: detalhe }));
}

export function renderizarIndicadores(container, dados) {
    const tempoMedio = dados.tempoMedioResolucaoHoras == null
        ? '—'
        : `${formatoDecimal.format(dados.tempoMedioResolucaoHoras)} h`;

    const cartoes = [
        criarCartao('Total de chamados', formatoNumero.format(dados.total), 'No período filtrado'),
        criarCartao('Abertos', formatoNumero.format(dados.abertos), 'Aguardando triagem'),
        criarCartao('Em andamento', formatoNumero.format(dados.emAndamento), 'Com técnico atuando'),
        criarCartao('Aguardando usuário', formatoNumero.format(dados.aguardandoUsuario), 'Dependem de retorno do solicitante'),
        criarCartao('Resolvidos', formatoNumero.format(dados.resolvidos),
            `${formatoDecimal.format(dados.taxaResolucao)}% do total`),
        criarCartao('Cancelados', formatoNumero.format(dados.cancelados), 'Encerrados sem solução'),
        criarCartao('Tempo médio de resolução', tempoMedio, 'Da abertura à conclusão'),
        criarCartao('Alta prioridade pendente', formatoNumero.format(dados.pendentesAltaPrioridade),
            'Prioridade alta ou crítica sem conclusão'),
    ];
    cartoes.at(-1).classList.add('indicador-critico');
    container.replaceChildren(...cartoes);
}

export function renderizarBarras(lista, itens) {
    const maior = Math.max(0, ...itens.map((item) => item.quantidade));
    if (maior === 0) {
        lista.replaceChildren(criarElemento('li', { class: 'vazio', texto: 'Sem dados para os filtros selecionados.' }));
        return;
    }

    lista.replaceChildren(...itens.map((item) => {
        const preenchimento = criarElemento('div', { class: 'barra-preenchimento' });
        preenchimento.style.width = `${(item.quantidade / maior) * 100}%`;

        return criarElemento('li', {},
            criarElemento('div', { class: 'barra-cabecalho' },
                criarElemento('span', { texto: item.rotulo }),
                criarElemento('span', {
                    class: 'barra-valor',
                    texto: `${formatoNumero.format(item.quantidade)} · ${formatoDecimal.format(item.percentual)}%`,
                })),
            criarElemento('div', { class: 'barra-trilha' }, preenchimento));
    }));
}

export function renderizarTabelaSerie(tabela, serie) {
    const cabecalho = criarElemento('thead', {},
        criarElemento('tr', {},
            criarElemento('th', { texto: 'Período' }),
            criarElemento('th', { texto: 'Abertos' }),
            criarElemento('th', { texto: 'Resolvidos' })));

    const corpo = criarElemento('tbody', {}, ...serie.map((ponto) =>
        criarElemento('tr', {},
            criarElemento('td', { texto: ponto.rotulo }),
            criarElemento('td', { class: 'numero', texto: formatoNumero.format(ponto.abertos) }),
            criarElemento('td', { class: 'numero', texto: formatoNumero.format(ponto.resolvidos) }))));

    tabela.replaceChildren(cabecalho, corpo);
}

export function renderizarChamados(corpo, chamados) {
    if (!chamados.length) {
        corpo.replaceChildren(criarElemento('tr', {},
            criarElemento('td', { class: 'vazio', colspan: '9', texto: 'Nenhum chamado encontrado para os filtros selecionados.' })));
        return;
    }
    corpo.replaceChildren(...chamados.map(criarLinhaChamado));
}

function criarLinhaChamado(chamado) {
    const botao = criarElemento('button', {
        type: 'button',
        class: 'botao botao-secundario botao-pequeno',
        'data-protocolo': chamado.protocolo,
        texto: 'Atualizar',
    });
    botao.disabled = STATUS_FINALIZADOS.has(chamado.status);

    const consideracoes = chamado.consideracoesTecnicas ?? '—';

    return criarElemento('tr', {},
        criarElemento('td', { class: 'protocolo', texto: chamado.protocolo }),
        criarElemento('td', { texto: chamado.titulo }),
        criarElemento('td', { texto: chamado.tipoDescricao }),
        criarElemento('td', { class: `prioridade-${chamado.prioridade}`, texto: chamado.prioridadeDescricao }),
        criarElemento('td', {},
            criarElemento('span', { class: `status status-${chamado.status}`, texto: chamado.statusDescricao })),
        criarElemento('td', { texto: chamado.tecnicoResponsavel ?? '—' }),
        criarElemento('td', { class: 'numero', texto: formatoDataHora.format(new Date(chamado.dataAbertura)) }),
        criarElemento('td', {},
            criarElemento('div', { class: 'consideracoes', title: consideracoes, texto: consideracoes })),
        criarElemento('td', {}, botao));
}

export function criarGraficoPeriodo(canvas) {
    if (!window.Chart) {
        return null;
    }

    const estilo = getComputedStyle(document.documentElement);
    const cor = (variavel) => estilo.getPropertyValue(variavel).trim();
    const serie = (rotulo, corSerie) => ({
        label: rotulo,
        data: [],
        borderColor: corSerie,
        backgroundColor: corSerie,
        pointBackgroundColor: corSerie,
        pointBorderColor: cor('--superficie'),
    });

    Chart.defaults.font.family = cor('--fonte');

    return new Chart(canvas, {
        type: 'line',
        data: {
            labels: [],
            datasets: [serie('Abertos', cor('--serie-1')), serie('Resolvidos', cor('--serie-2'))],
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            interaction: { mode: 'index', intersect: false },
            elements: {
                line: { borderWidth: 2, cubicInterpolationMode: 'monotone', borderCapStyle: 'round', borderJoinStyle: 'round' },
                point: { radius: 0, hoverRadius: 5, hoverBorderWidth: 2, hitRadius: 12 },
            },
            plugins: {
                legend: {
                    align: 'start',
                    labels: { color: cor('--texto-secundario'), usePointStyle: true, pointStyle: 'circle', boxHeight: 8 },
                },
                tooltip: { backgroundColor: cor('--texto'), padding: 10, boxPadding: 4, usePointStyle: true },
            },
            scales: {
                x: {
                    grid: { display: false },
                    border: { color: cor('--linha-base') },
                    ticks: { color: cor('--texto-suave'), maxRotation: 0, autoSkipPadding: 16 },
                },
                y: {
                    beginAtZero: true,
                    grid: { color: cor('--grade') },
                    border: { display: false },
                    ticks: { color: cor('--texto-suave'), precision: 0 },
                },
            },
        },
    });
}

export function atualizarGraficoPeriodo(grafico, serie) {
    if (!grafico) {
        return;
    }
    grafico.data.labels = serie.map((ponto) => ponto.rotulo);
    grafico.data.datasets[0].data = serie.map((ponto) => ponto.abertos);
    grafico.data.datasets[1].data = serie.map((ponto) => ponto.resolvidos);
    grafico.update();
}