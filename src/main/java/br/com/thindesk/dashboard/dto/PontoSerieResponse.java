package br.com.thindesk.dashboard.dto;

import java.time.LocalDate;

public record PontoSerieResponse(
        LocalDate inicio,
        String rotulo,
        long abertos,
        long resolvidos) {
}