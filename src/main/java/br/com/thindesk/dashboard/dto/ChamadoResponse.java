package br.com.thindesk.dashboard.dto;

import br.com.thindesk.dashboard.model.Chamado;
import br.com.thindesk.dashboard.model.Prioridade;
import br.com.thindesk.dashboard.model.StatusChamado;
import br.com.thindesk.dashboard.model.TipoChamado;

import java.time.Duration;
import java.time.LocalDateTime;

public record ChamadoResponse(
        String protocolo,
        String titulo,
        String descricao,
        TipoChamado tipo,
        String tipoDescricao,
        Prioridade prioridade,
        String prioridadeDescricao,
        StatusChamado status,
        String statusDescricao,
        String solicitante,
        String setor,
        String tecnicoResponsavel,
        String consideracoesTecnicas,
        LocalDateTime dataAbertura,
        LocalDateTime dataInicioAtendimento,
        LocalDateTime dataConclusao,
        Long tempoAtendimentoMinutos) {

    public static ChamadoResponse de(Chamado chamado) {
        Long tempoAtendimento = chamado.getDataConclusao() == null
                ? null
                : Duration.between(chamado.getDataAbertura(), chamado.getDataConclusao()).toMinutes();

        return new ChamadoResponse(
                chamado.getProtocolo(),
                chamado.getTitulo(),
                chamado.getDescricao(),
                chamado.getTipo(),
                chamado.getTipo().getDescricao(),
                chamado.getPrioridade(),
                chamado.getPrioridade().getDescricao(),
                chamado.getStatus(),
                chamado.getStatus().getDescricao(),
                chamado.getSolicitante(),
                chamado.getSetor(),
                chamado.getTecnicoResponsavel(),
                chamado.getConsideracoesTecnicas(),
                chamado.getDataAbertura(),
                chamado.getDataInicioAtendimento(),
                chamado.getDataConclusao(),
                tempoAtendimento);
    }
}