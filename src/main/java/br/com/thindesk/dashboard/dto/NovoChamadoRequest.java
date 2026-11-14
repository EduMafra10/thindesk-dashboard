package br.com.thindesk.dashboard.dto;

import br.com.thindesk.dashboard.model.Prioridade;
import br.com.thindesk.dashboard.model.TipoChamado;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NovoChamadoRequest(
        @NotBlank(message = "O título é obrigatório.")
        @Size(max = 120, message = "O título deve ter no máximo 120 caracteres.")
        String titulo,

        @NotBlank(message = "A descrição é obrigatória.")
        @Size(max = 2000, message = "A descrição deve ter no máximo 2000 caracteres.")
        String descricao,

        @NotNull(message = "O tipo é obrigatório.")
        TipoChamado tipo,

        @NotNull(message = "A prioridade é obrigatória.")
        Prioridade prioridade,

        @NotBlank(message = "O solicitante é obrigatório.")
        @Size(max = 100, message = "O solicitante deve ter no máximo 100 caracteres.")
        String solicitante,

        @NotBlank(message = "O setor é obrigatório.")
        @Size(max = 60, message = "O setor deve ter no máximo 60 caracteres.")
        String setor) {
}