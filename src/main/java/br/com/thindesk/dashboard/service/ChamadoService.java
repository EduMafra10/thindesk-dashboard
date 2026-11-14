package br.com.thindesk.dashboard.service;

import br.com.thindesk.dashboard.dto.AtualizacaoStatusRequest;
import br.com.thindesk.dashboard.dto.ChamadoFiltro;
import br.com.thindesk.dashboard.dto.ChamadoResponse;
import br.com.thindesk.dashboard.dto.NovoChamadoRequest;
import br.com.thindesk.dashboard.dto.PaginaResponse;
import br.com.thindesk.dashboard.exception.RecursoNaoEncontradoException;
import br.com.thindesk.dashboard.exception.RegraNegocioException;
import br.com.thindesk.dashboard.model.Chamado;
import br.com.thindesk.dashboard.repository.ChamadoCriteria;
import br.com.thindesk.dashboard.repository.ChamadoRepository;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChamadoService {

    private static final String PREFIXO_PROTOCOLO = "TD-";

    private final ChamadoRepository repository;
    private final MongoTemplate mongoTemplate;

    public ChamadoService(ChamadoRepository repository, MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    public PaginaResponse<ChamadoResponse> listar(ChamadoFiltro filtro, int pagina, int tamanho) {
        Pageable paginacao = PageRequest.of(pagina, tamanho, Sort.by(Sort.Direction.DESC, "dataAbertura"));
        Query consulta = Query.query(ChamadoCriteria.aPartirDe(filtro));

        long total = mongoTemplate.count(consulta, Chamado.class);
        List<ChamadoResponse> chamados = mongoTemplate.find(consulta.with(paginacao), Chamado.class)
                .stream()
                .map(ChamadoResponse::de)
                .toList();

        return PaginaResponse.de(new PageImpl<>(chamados, paginacao, total));
    }

    public ChamadoResponse buscarPorProtocolo(String protocolo) {
        return ChamadoResponse.de(buscar(protocolo));
    }

    public ChamadoResponse abrir(NovoChamadoRequest request) {
        Chamado chamado = new Chamado(
                proximoProtocolo(),
                request.titulo().trim(),
                request.descricao().trim(),
                request.tipo(),
                request.prioridade(),
                request.solicitante().trim(),
                request.setor().trim(),
                LocalDateTime.now());

        return ChamadoResponse.de(repository.save(chamado));
    }

    public ChamadoResponse atualizarStatus(String protocolo, AtualizacaoStatusRequest request) {
        Chamado chamado = buscar(protocolo);
        LocalDateTime agora = LocalDateTime.now();

        switch (request.status()) {
            case EM_ANDAMENTO -> chamado.iniciarAtendimento(request.tecnico(), agora);
            case AGUARDANDO_USUARIO -> chamado.aguardarUsuario(agora);
            case RESOLVIDO -> chamado.resolver(request.consideracoes(), agora);
            case CANCELADO -> chamado.cancelar(request.consideracoes(), agora);
            case ABERTO -> throw new RegraNegocioException("Um chamado não pode retornar ao status Aberto.");
        }

        return ChamadoResponse.de(repository.save(chamado));
    }

    private Chamado buscar(String protocolo) {
        return repository.findByProtocolo(protocolo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Chamado " + protocolo + " não encontrado."));
    }

    private String proximoProtocolo() {
        int ultimoNumero = repository.findTopByOrderByProtocoloDesc()
                .map(chamado -> Integer.parseInt(chamado.getProtocolo().substring(PREFIXO_PROTOCOLO.length())))
                .orElse(0);
        return PREFIXO_PROTOCOLO + "%06d".formatted(ultimoNumero + 1);
    }
}