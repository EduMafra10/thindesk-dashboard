package br.com.thindesk.dashboard.dto;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;

public enum Agrupamento {

    DIA("Dia", "dd/MM"),
    SEMANA("Semana", "dd/MM"),
    MES("Mês", "MM/yyyy");

    private final String descricao;
    private final DateTimeFormatter formato;

    Agrupamento(String descricao, String padraoRotulo) {
        this.descricao = descricao;
        this.formato = DateTimeFormatter.ofPattern(padraoRotulo);
    }

    public LocalDate inicioDoPeriodo(LocalDate data) {
        return switch (this) {
            case DIA -> data;
            case SEMANA -> data.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MES -> data.withDayOfMonth(1);
        };
    }

    public LocalDate proximoPeriodo(LocalDate inicio) {
        return switch (this) {
            case DIA -> inicio.plusDays(1);
            case SEMANA -> inicio.plusWeeks(1);
            case MES -> inicio.plusMonths(1);
        };
    }

    public String rotulo(LocalDate inicio) {
        return inicio.format(formato);
    }

    public String getDescricao() {
        return descricao;
    }
}