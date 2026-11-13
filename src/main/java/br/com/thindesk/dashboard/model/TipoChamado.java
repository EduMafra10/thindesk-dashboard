package br.com.thindesk.dashboard.model;

public enum TipoChamado {

    HARDWARE("Hardware"),
    SOFTWARE("Software"),
    REDE("Rede e conectividade"),
    ACESSO("Acessos e senhas"),
    IMPRESSORA("Impressoras"),
    EMAIL("E-mail"),
    SISTEMA_INTERNO("Sistemas internos");

    private final String descricao;

    TipoChamado(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}