package br.com.thindesk.dashboard.dto;

import br.com.thindesk.dashboard.model.StatusChamado;

public record ContagemStatusResponse(
        StatusChamado status,
        String descricao,
        long quantidade,
        double percentual) {
}