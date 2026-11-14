package br.com.thindesk.dashboard.controller;

import br.com.thindesk.dashboard.dto.Agrupamento;
import br.com.thindesk.dashboard.dto.ChamadoFiltro;
import br.com.thindesk.dashboard.dto.ContagemStatusResponse;
import br.com.thindesk.dashboard.dto.IndicadoresResponse;
import br.com.thindesk.dashboard.dto.OpcoesFiltroResponse;
import br.com.thindesk.dashboard.dto.PontoSerieResponse;
import br.com.thindesk.dashboard.dto.RankingTipoResponse;
import br.com.thindesk.dashboard.service.DashboardService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping("/indicadores")
    public IndicadoresResponse indicadores(ChamadoFiltro filtro) {
        return service.indicadores(filtro);
    }

    @GetMapping("/atendimentos-por-periodo")
    public List<PontoSerieResponse> atendimentosPorPeriodo(
            ChamadoFiltro filtro,
            @RequestParam(defaultValue = "SEMANA") Agrupamento agrupamento) {
        return service.atendimentosPorPeriodo(filtro, agrupamento);
    }

    @GetMapping("/status")
    public List<ContagemStatusResponse> status(ChamadoFiltro filtro) {
        return service.situacao(filtro);
    }

    @GetMapping("/tipos-mais-atendidos")
    public List<RankingTipoResponse> tiposMaisAtendidos(
            ChamadoFiltro filtro,
            @RequestParam(defaultValue = "5")
            @Min(value = 1, message = "O limite mínimo é 1.")
            @Max(value = 7, message = "O limite máximo é 7.") int limite) {
        return service.tiposMaisAtendidos(filtro, limite);
    }

    @GetMapping("/opcoes-filtro")
    public OpcoesFiltroResponse opcoesFiltro() {
        return service.opcoesFiltro();
    }
}