package br.com.thindesk.dashboard.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class AgrupamentoTest {

    private static final LocalDate QUINTA_FEIRA = LocalDate.of(2026, 11, 12);

    @Test
    @DisplayName("Agrupamento diário mantém a própria data")
    void dia() {
        assertThat(Agrupamento.DIA.inicioDoPeriodo(QUINTA_FEIRA)).isEqualTo(QUINTA_FEIRA);
    }

    @Test
    @DisplayName("Agrupamento semanal começa na segunda-feira")
    void semanaComecaNaSegunda() {
        assertThat(Agrupamento.SEMANA.inicioDoPeriodo(QUINTA_FEIRA)).isEqualTo(LocalDate.of(2026, 11, 9));
    }

    @Test
    @DisplayName("Agrupamento mensal começa no primeiro dia do mês")
    void mesComecaNoDiaUm() {
        assertThat(Agrupamento.MES.inicioDoPeriodo(QUINTA_FEIRA)).isEqualTo(LocalDate.of(2026, 11, 1));
    }

    @Test
    @DisplayName("O próximo período mensal avança exatamente um mês")
    void proximoMes() {
        assertThat(Agrupamento.MES.proximoPeriodo(LocalDate.of(2026, 11, 1))).isEqualTo(LocalDate.of(2026, 12, 1));
    }

    @Test
    @DisplayName("Os rótulos seguem o formato brasileiro")
    void rotulos() {
        assertThat(Agrupamento.SEMANA.rotulo(LocalDate.of(2026, 11, 9))).isEqualTo("09/11");
        assertThat(Agrupamento.MES.rotulo(LocalDate.of(2026, 11, 1))).isEqualTo("11/2026");
    }
}