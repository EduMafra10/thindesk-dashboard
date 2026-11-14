package br.com.thindesk.dashboard.repository;

import br.com.thindesk.dashboard.dto.ChamadoFiltro;
import br.com.thindesk.dashboard.exception.RegraNegocioException;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.ArrayList;
import java.util.List;

public final class ChamadoCriteria {

    private ChamadoCriteria() {
    }

    public static Criteria aPartirDe(ChamadoFiltro filtro) {
        validarPeriodo(filtro);
        List<Criteria> condicoes = new ArrayList<>();

        if (filtro.status() != null) {
            condicoes.add(Criteria.where("status").is(filtro.status()));
        }
        if (filtro.tipo() != null) {
            condicoes.add(Criteria.where("tipo").is(filtro.tipo()));
        }
        if (filtro.prioridade() != null) {
            condicoes.add(Criteria.where("prioridade").is(filtro.prioridade()));
        }
        if (filtro.tecnico() != null && !filtro.tecnico().isBlank()) {
            condicoes.add(Criteria.where("tecnicoResponsavel").is(filtro.tecnico().trim()));
        }
        if (filtro.dataInicio() != null) {
            condicoes.add(Criteria.where("dataAbertura").gte(filtro.dataInicio().atStartOfDay()));
        }
        if (filtro.dataFim() != null) {
            condicoes.add(Criteria.where("dataAbertura").lt(filtro.dataFim().plusDays(1).atStartOfDay()));
        }

        return condicoes.isEmpty() ? new Criteria() : new Criteria().andOperator(condicoes);
    }

    private static void validarPeriodo(ChamadoFiltro filtro) {
        if (filtro.dataInicio() != null && filtro.dataFim() != null
                && filtro.dataInicio().isAfter(filtro.dataFim())) {
            throw new RegraNegocioException("A data inicial não pode ser posterior à data final.");
        }
    }
}