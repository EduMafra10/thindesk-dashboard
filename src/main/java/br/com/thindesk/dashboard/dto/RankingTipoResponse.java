package br.com.thindesk.dashboard.dto;

import br.com.thindesk.dashboard.model.TipoChamado;

public record RankingTipoResponse(
        int posicao,
        TipoChamado tipo,
        String descricao,
        long quantidade,
        double percentual) {
}