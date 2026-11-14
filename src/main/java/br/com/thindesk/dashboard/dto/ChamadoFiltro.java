package br.com.thindesk.dashboard.dto;

import br.com.thindesk.dashboard.model.Prioridade;
import br.com.thindesk.dashboard.model.StatusChamado;
import br.com.thindesk.dashboard.model.TipoChamado;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record ChamadoFiltro(
        StatusChamado status,
        TipoChamado tipo,
        Prioridade prioridade,
        String tecnico,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
}