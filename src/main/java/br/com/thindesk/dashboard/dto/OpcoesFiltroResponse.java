package br.com.thindesk.dashboard.dto;

import java.util.List;

public record OpcoesFiltroResponse(
        List<Opcao> status,
        List<Opcao> tipos,
        List<Opcao> prioridades,
        List<Opcao> agrupamentos,
        List<String> tecnicos) {

    public record Opcao(String valor, String descricao) {
    }
}