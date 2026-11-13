package br.com.thindesk.dashboard.model;

public enum StatusChamado {

    ABERTO("Aberto"),
    EM_ANDAMENTO("Em andamento"),
    AGUARDANDO_USUARIO("Aguardando usuário"),
    RESOLVIDO("Resolvido"),
    CANCELADO("Cancelado");

    private final String descricao;

    StatusChamado(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    public boolean isFinalizado() {
        return this == RESOLVIDO || this == CANCELADO;
    }
}