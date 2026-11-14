package br.com.thindesk.dashboard.dto;

public record IndicadoresResponse(
        long total,
        long abertos,
        long emAndamento,
        long aguardandoUsuario,
        long resolvidos,
        long cancelados,
        double taxaResolucao,
        Double tempoMedioResolucaoHoras,
        long pendentesAltaPrioridade) {
}