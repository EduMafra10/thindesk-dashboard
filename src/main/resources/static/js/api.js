const BASE_API = '/api';
const MENSAGEM_SEM_CONEXAO = 'Não foi possível conectar à API. Verifique se a aplicação está em execução.';

export class ErroApi extends Error {
    constructor(mensagem, status) {
        super(mensagem);
        this.status = status;
    }
}

function montarMensagem(problema) {
    if (!problema) {
        return 'Ocorreu um erro inesperado.';
    }
    const detalhes = (problema.erros ?? []).map((erro) => erro.mensagem);
    return detalhes.length ? detalhes.join(' ') : problema.detail;
}

async function requisitar(caminho, { metodo = 'GET', corpo } = {}) {
    let resposta;
    try {
        resposta = await fetch(BASE_API + caminho, {
            method: metodo,
            headers: corpo
                ? { Accept: 'application/json', 'Content-Type': 'application/json' }
                : { Accept: 'application/json' },
            body: corpo ? JSON.stringify(corpo) : undefined,
        });
    } catch {
        throw new ErroApi(MENSAGEM_SEM_CONEXAO, 0);
    }

    const conteudo = await resposta.json().catch(() => null);
    if (!resposta.ok) {
        throw new ErroApi(montarMensagem(conteudo), resposta.status);
    }
    return conteudo;
}

function comParametros(caminho, ...grupos) {
    const parametros = new URLSearchParams();
    grupos.forEach((grupo) => {
        Object.entries(grupo).forEach(([chave, valor]) => {
            if (valor !== '' && valor !== null && valor !== undefined) {
                parametros.append(chave, valor);
            }
        });
    });
    const consulta = parametros.toString();
    return consulta ? `${caminho}?${consulta}` : caminho;
}

export const api = {
    opcoesFiltro: () => requisitar('/dashboard/opcoes-filtro'),
    indicadores: (filtros) => requisitar(comParametros('/dashboard/indicadores', filtros)),
    serie: (filtros, agrupamento) =>
        requisitar(comParametros('/dashboard/atendimentos-por-periodo', filtros, { agrupamento })),
    status: (filtros) => requisitar(comParametros('/dashboard/status', filtros)),
    tiposMaisAtendidos: (filtros) =>
        requisitar(comParametros('/dashboard/tipos-mais-atendidos', filtros, { limite: 5 })),
    chamados: (filtros, pagina) =>
        requisitar(comParametros('/chamados', filtros, { pagina, tamanho: 10 })),
    atualizarStatus: (protocolo, dados) =>
        requisitar(`/chamados/${encodeURIComponent(protocolo)}/status`, { metodo: 'PATCH', corpo: dados }),
};