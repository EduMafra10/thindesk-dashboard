package br.com.thindesk.dashboard.controller;

import br.com.thindesk.dashboard.dto.AtualizacaoStatusRequest;
import br.com.thindesk.dashboard.dto.ChamadoFiltro;
import br.com.thindesk.dashboard.dto.ChamadoResponse;
import br.com.thindesk.dashboard.dto.NovoChamadoRequest;
import br.com.thindesk.dashboard.dto.PaginaResponse;
import br.com.thindesk.dashboard.service.ChamadoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/chamados")
public class ChamadoController {

    private static final String PADRAO_PROTOCOLO = "TD-\\d{6}";
    private static final String MENSAGEM_PROTOCOLO = "Protocolo inválido. Use o formato TD-000000.";

    private final ChamadoService service;

    public ChamadoController(ChamadoService service) {
        this.service = service;
    }

    @GetMapping
    public PaginaResponse<ChamadoResponse> listar(
            ChamadoFiltro filtro,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "A página não pode ser negativa.") int pagina,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "O tamanho mínimo é 1.")
            @Max(value = 100, message = "O tamanho máximo é 100.") int tamanho) {
        return service.listar(filtro, pagina, tamanho);
    }

    @GetMapping("/{protocolo}")
    public ChamadoResponse buscar(
            @PathVariable @Pattern(regexp = PADRAO_PROTOCOLO, message = MENSAGEM_PROTOCOLO) String protocolo) {
        return service.buscarPorProtocolo(protocolo);
    }

    @PostMapping
    public ResponseEntity<ChamadoResponse> abrir(
            @RequestBody @Valid NovoChamadoRequest request,
            UriComponentsBuilder uriBuilder) {
        ChamadoResponse criado = service.abrir(request);
        URI localizacao = uriBuilder.path("/api/chamados/{protocolo}")
                .buildAndExpand(criado.protocolo())
                .toUri();
        return ResponseEntity.created(localizacao).body(criado);
    }

    @PatchMapping("/{protocolo}/status")
    public ChamadoResponse atualizarStatus(
            @PathVariable @Pattern(regexp = PADRAO_PROTOCOLO, message = MENSAGEM_PROTOCOLO) String protocolo,
            @RequestBody @Valid AtualizacaoStatusRequest request) {
        return service.atualizarStatus(protocolo, request);
    }
}