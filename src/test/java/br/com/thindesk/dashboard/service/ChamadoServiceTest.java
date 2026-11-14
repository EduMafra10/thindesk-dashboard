package br.com.thindesk.dashboard.service;

import br.com.thindesk.dashboard.dto.AtualizacaoStatusRequest;
import br.com.thindesk.dashboard.dto.ChamadoResponse;
import br.com.thindesk.dashboard.dto.NovoChamadoRequest;
import br.com.thindesk.dashboard.exception.RecursoNaoEncontradoException;
import br.com.thindesk.dashboard.exception.RegraNegocioException;
import br.com.thindesk.dashboard.model.Chamado;
import br.com.thindesk.dashboard.model.Prioridade;
import br.com.thindesk.dashboard.model.StatusChamado;
import br.com.thindesk.dashboard.model.TipoChamado;
import br.com.thindesk.dashboard.repository.ChamadoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChamadoServiceTest {

    @Mock
    private ChamadoRepository repository;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private ChamadoService service;

    private final NovoChamadoRequest requisicao = new NovoChamadoRequest(
            "Notebook sem áudio", "Sem som após a atualização do sistema.",
            TipoChamado.HARDWARE, Prioridade.MEDIA, "Ana Ribeiro", "Comercial");

    private static Chamado chamado(String protocolo) {
        return new Chamado(protocolo, "Senha expirada", "Usuário não consegue acessar o sistema.",
                TipoChamado.ACESSO, Prioridade.ALTA, "Bruno Carvalho", "Jurídico",
                LocalDateTime.of(2026, 11, 13, 10, 0));
    }

    @Test
    @DisplayName("Abrir chamado gera o próximo protocolo da sequência")
    void abrirGeraProximoProtocolo() {
        when(repository.findTopByOrderByProtocoloDesc()).thenReturn(Optional.of(chamado("TD-000321")));
        when(repository.save(any(Chamado.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        ChamadoResponse resposta = service.abrir(requisicao);

        assertThat(resposta.protocolo()).isEqualTo("TD-000322");
        assertThat(resposta.status()).isEqualTo(StatusChamado.ABERTO);
    }

    @Test
    @DisplayName("O primeiro chamado cadastrado recebe o protocolo TD-000001")
    void primeiroProtocolo() {
        when(repository.findTopByOrderByProtocoloDesc()).thenReturn(Optional.empty());
        when(repository.save(any(Chamado.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        assertThat(service.abrir(requisicao).protocolo()).isEqualTo("TD-000001");
    }

    @Test
    @DisplayName("Buscar um protocolo inexistente lança exceção de recurso não encontrado")
    void protocoloInexistente() {
        when(repository.findByProtocolo("TD-999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorProtocolo("TD-999999"))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("Atualizar para Em andamento registra o técnico informado")
    void atualizarParaEmAndamento() {
        Chamado chamado = chamado("TD-000010");
        when(repository.findByProtocolo("TD-000010")).thenReturn(Optional.of(chamado));
        when(repository.save(chamado)).thenReturn(chamado);

        ChamadoResponse resposta = service.atualizarStatus("TD-000010",
                new AtualizacaoStatusRequest(StatusChamado.EM_ANDAMENTO, "Carlos Mendes", null));

        assertThat(resposta.status()).isEqualTo(StatusChamado.EM_ANDAMENTO);
        assertThat(resposta.tecnicoResponsavel()).isEqualTo("Carlos Mendes");
    }

    @Test
    @DisplayName("Não é permitido retornar um chamado ao status Aberto")
    void naoPermiteVoltarParaAberto() {
        when(repository.findByProtocolo("TD-000010")).thenReturn(Optional.of(chamado("TD-000010")));

        assertThatThrownBy(() -> service.atualizarStatus("TD-000010",
                new AtualizacaoStatusRequest(StatusChamado.ABERTO, null, null)))
                .isInstanceOf(RegraNegocioException.class);
        verify(repository, never()).save(any());
    }
}