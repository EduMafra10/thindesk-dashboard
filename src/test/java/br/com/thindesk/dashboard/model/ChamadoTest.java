package br.com.thindesk.dashboard.model;

import br.com.thindesk.dashboard.exception.RegraNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChamadoTest {

    private static final LocalDateTime ABERTURA = LocalDateTime.of(2026, 11, 10, 9, 0);

    private Chamado novoChamado() {
        return new Chamado("TD-000001", "Impressora não imprime", "Fila de impressão travada.",
                TipoChamado.IMPRESSORA, Prioridade.MEDIA, "Ana Ribeiro", "Financeiro", ABERTURA);
    }

    @Test
    @DisplayName("Um chamado novo começa com status Aberto e sem técnico")
    void chamadoNovoComecaAberto() {
        Chamado chamado = novoChamado();

        assertThat(chamado.getStatus()).isEqualTo(StatusChamado.ABERTO);
        assertThat(chamado.getTecnicoResponsavel()).isNull();
    }

    @Test
    @DisplayName("Iniciar atendimento registra o técnico e a data de início")
    void iniciarAtendimento() {
        Chamado chamado = novoChamado();

        chamado.iniciarAtendimento("Carlos Mendes", ABERTURA.plusHours(1));

        assertThat(chamado.getStatus()).isEqualTo(StatusChamado.EM_ANDAMENTO);
        assertThat(chamado.getTecnicoResponsavel()).isEqualTo("Carlos Mendes");
        assertThat(chamado.getDataInicioAtendimento()).isEqualTo(ABERTURA.plusHours(1));
    }

    @Test
    @DisplayName("Não é possível iniciar atendimento sem técnico responsável")
    void iniciarSemTecnico() {
        Chamado chamado = novoChamado();

        assertThatThrownBy(() -> chamado.iniciarAtendimento(" ", ABERTURA))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("técnico responsável");
    }

    @Test
    @DisplayName("Retomar o atendimento preserva a data de início original")
    void retomarAtendimento() {
        Chamado chamado = novoChamado();
        chamado.iniciarAtendimento("Carlos Mendes", ABERTURA.plusHours(1));
        chamado.aguardarUsuario(ABERTURA.plusHours(2));

        chamado.iniciarAtendimento("Juliana Prado", ABERTURA.plusHours(5));

        assertThat(chamado.getStatus()).isEqualTo(StatusChamado.EM_ANDAMENTO);
        assertThat(chamado.getTecnicoResponsavel()).isEqualTo("Juliana Prado");
        assertThat(chamado.getDataInicioAtendimento()).isEqualTo(ABERTURA.plusHours(1));
    }

    @Test
    @DisplayName("Não é possível resolver um chamado que não está em atendimento")
    void resolverSemAtendimento() {
        Chamado chamado = novoChamado();

        assertThatThrownBy(() -> chamado.resolver("Spooler reiniciado.", ABERTURA.plusHours(2)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("não está em atendimento");
    }

    @Test
    @DisplayName("Não é possível resolver um chamado sem considerações técnicas")
    void resolverSemConsideracoes() {
        Chamado chamado = novoChamado();
        chamado.iniciarAtendimento("Carlos Mendes", ABERTURA.plusHours(1));

        assertThatThrownBy(() -> chamado.resolver("", ABERTURA.plusHours(2)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("considerações técnicas");
    }

    @Test
    @DisplayName("Resolver registra as considerações técnicas e a data de conclusão")
    void resolver() {
        Chamado chamado = novoChamado();
        chamado.iniciarAtendimento("Carlos Mendes", ABERTURA.plusHours(1));

        chamado.resolver("Spooler reiniciado e fila liberada.", ABERTURA.plusHours(3));

        assertThat(chamado.getStatus()).isEqualTo(StatusChamado.RESOLVIDO);
        assertThat(chamado.getConsideracoesTecnicas()).isEqualTo("Spooler reiniciado e fila liberada.");
        assertThat(chamado.getDataConclusao()).isEqualTo(ABERTURA.plusHours(3));
    }

    @Test
    @DisplayName("Um chamado finalizado não pode ser alterado")
    void chamadoFinalizadoNaoPodeSerAlterado() {
        Chamado chamado = novoChamado();
        chamado.iniciarAtendimento("Carlos Mendes", ABERTURA.plusHours(1));
        chamado.resolver("Spooler reiniciado.", ABERTURA.plusHours(3));

        assertThatThrownBy(() -> chamado.cancelar("Cancelamento indevido.", ABERTURA.plusHours(4)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("já foi finalizado");
    }
}