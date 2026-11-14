package br.com.thindesk.dashboard.dto;

import br.com.thindesk.dashboard.model.StatusChamado;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AtualizacaoStatusRequest(
        @NotNull(message = "Informe o novo status.")
        StatusChamado status,

        @Size(max = 100, message = "O nome do técnico deve ter no máximo 100 caracteres.")
        String tecnico,

        @Size(max = 2000, message = "As considerações devem ter no máximo 2000 caracteres.")
        String consideracoes) {
}