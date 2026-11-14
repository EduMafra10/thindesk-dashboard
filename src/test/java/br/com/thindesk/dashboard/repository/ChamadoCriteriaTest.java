package br.com.thindesk.dashboard.repository;

import br.com.thindesk.dashboard.dto.ChamadoFiltro;
import br.com.thindesk.dashboard.exception.RegraNegocioException;
import org.bson.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChamadoCriteriaTest {

    @Test
    @DisplayName("Rejeita período com data inicial posterior à data final")
    void periodoInvertido() {
        ChamadoFiltro filtro = new ChamadoFiltro(null, null, null, null,
                LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 1));

        assertThatThrownBy(() -> ChamadoCriteria.aPartirDe(filtro))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    @DisplayName("A data final inclui o dia inteiro na consulta")
    void dataFinalInclusiva() {
        ChamadoFiltro filtro = new ChamadoFiltro(null, null, null, null, null, LocalDate.of(2026, 11, 14));

        Document consulta = ChamadoCriteria.aPartirDe(filtro).getCriteriaObject();
        Document condicao = consulta.getList("$and", Document.class).get(0).get("dataAbertura", Document.class);

        assertThat(condicao.get("$lt")).isEqualTo(LocalDate.of(2026, 11, 15).atStartOfDay());
    }
}