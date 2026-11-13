package br.com.thindesk.dashboard.model;

import br.com.thindesk.dashboard.exception.RegraNegocioException;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "chamados")
@CompoundIndex(name = "status_data_abertura", def = "{'status': 1, 'dataAbertura': -1}")
public class Chamado {

    @Id
    private String id;

    @Indexed(unique = true)
    private String protocolo;

    private String titulo;
    private String descricao;

    @Indexed
    private TipoChamado tipo;

    private Prioridade prioridade;
    private StatusChamado status;
    private String solicitante;
    private String setor;

    @Indexed
    private String tecnicoResponsavel;

    private String consideracoesTecnicas;

    @Indexed
    private LocalDateTime dataAbertura;

    private LocalDateTime dataInicioAtendimento;
    private LocalDateTime dataConclusao;
    private LocalDateTime atualizadoEm;

    protected Chamado() {
    }

    public Chamado(String protocolo, String titulo, String descricao, TipoChamado tipo,
                   Prioridade prioridade, String solicitante, String setor, LocalDateTime dataAbertura) {
        this.protocolo = protocolo;
        this.titulo = titulo;
        this.descricao = descricao;
        this.tipo = tipo;
        this.prioridade = prioridade;
        this.solicitante = solicitante;
        this.setor = setor;
        this.dataAbertura = dataAbertura;
        this.status = StatusChamado.ABERTO;
        this.atualizadoEm = dataAbertura;
    }

    public void iniciarAtendimento(String tecnico, LocalDateTime momento) {
        garantirNaoFinalizado();
        if (tecnico == null || tecnico.isBlank()) {
            throw new RegraNegocioException("Informe o técnico responsável pelo atendimento.");
        }
        this.tecnicoResponsavel = tecnico;
        if (dataInicioAtendimento == null) {
            this.dataInicioAtendimento = momento;
        }
        this.status = StatusChamado.EM_ANDAMENTO;
        this.atualizadoEm = momento;
    }

    public void aguardarUsuario(LocalDateTime momento) {
        garantirEmAtendimento();
        this.status = StatusChamado.AGUARDANDO_USUARIO;
        this.atualizadoEm = momento;
    }

    public void resolver(String consideracoes, LocalDateTime momento) {
        garantirEmAtendimento();
        if (consideracoes == null || consideracoes.isBlank()) {
            throw new RegraNegocioException("Informe as considerações técnicas para resolver o chamado.");
        }
        finalizar(StatusChamado.RESOLVIDO, consideracoes, momento);
    }

    public void cancelar(String motivo, LocalDateTime momento) {
        garantirNaoFinalizado();
        finalizar(StatusChamado.CANCELADO, motivo, momento);
    }

    private void finalizar(StatusChamado statusFinal, String consideracoes, LocalDateTime momento) {
        this.status = statusFinal;
        this.consideracoesTecnicas = consideracoes;
        this.dataConclusao = momento;
        this.atualizadoEm = momento;
    }

    private void garantirNaoFinalizado() {
        if (status.isFinalizado()) {
            throw new RegraNegocioException("O chamado " + protocolo + " já foi finalizado.");
        }
    }

    private void garantirEmAtendimento() {
        if (status != StatusChamado.EM_ANDAMENTO && status != StatusChamado.AGUARDANDO_USUARIO) {
            throw new RegraNegocioException("O chamado " + protocolo + " não está em atendimento.");
        }
    }

    public String getId() {
        return id;
    }

    public String getProtocolo() {
        return protocolo;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public TipoChamado getTipo() {
        return tipo;
    }

    public Prioridade getPrioridade() {
        return prioridade;
    }

    public StatusChamado getStatus() {
        return status;
    }

    public String getSolicitante() {
        return solicitante;
    }

    public String getSetor() {
        return setor;
    }

    public String getTecnicoResponsavel() {
        return tecnicoResponsavel;
    }

    public String getConsideracoesTecnicas() {
        return consideracoesTecnicas;
    }

    public LocalDateTime getDataAbertura() {
        return dataAbertura;
    }

    public LocalDateTime getDataInicioAtendimento() {
        return dataInicioAtendimento;
    }

    public LocalDateTime getDataConclusao() {
        return dataConclusao;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }
}