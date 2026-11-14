package br.com.thindesk.dashboard.controller;

import br.com.thindesk.dashboard.dto.ChamadoResponse;
import br.com.thindesk.dashboard.exception.RecursoNaoEncontradoException;
import br.com.thindesk.dashboard.exception.RegraNegocioException;
import br.com.thindesk.dashboard.exception.TratadorGlobalDeErros;
import br.com.thindesk.dashboard.model.Chamado;
import br.com.thindesk.dashboard.model.Prioridade;
import br.com.thindesk.dashboard.model.TipoChamado;
import br.com.thindesk.dashboard.service.ChamadoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItems;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ChamadoControllerTest {

    private final ChamadoService service = mock(ChamadoService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void configurar() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ChamadoController(service))
                .setControllerAdvice(new TratadorGlobalDeErros())
                .build();
    }

    private static ChamadoResponse chamadoCriado() {
        return ChamadoResponse.de(new Chamado("TD-000322", "Notebook sem audio", "Sem som apos atualizacao.",
                TipoChamado.HARDWARE, Prioridade.MEDIA, "Ana Ribeiro", "Comercial",
                LocalDateTime.of(2026, 11, 14, 9, 0)));
    }

    @Test
    @DisplayName("POST /api/chamados retorna 201 com o cabeçalho Location")
    void abrirChamado() throws Exception {
        when(service.abrir(any())).thenReturn(chamadoCriado());

        mockMvc.perform(post("/api/chamados")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titulo": "Notebook sem audio",
                                  "descricao": "Sem som apos atualizacao.",
                                  "tipo": "HARDWARE",
                                  "prioridade": "MEDIA",
                                  "solicitante": "Ana Ribeiro",
                                  "setor": "Comercial"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/chamados/TD-000322")))
                .andExpect(jsonPath("$.protocolo").value("TD-000322"))
                .andExpect(jsonPath("$.status").value("ABERTO"));
    }

    @Test
    @DisplayName("POST com campos obrigatórios ausentes retorna 400 com a lista de erros")
    void abrirChamadoInvalido() throws Exception {
        mockMvc.perform(post("/api/chamados")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$..campo", hasItems("titulo", "descricao", "tipo", "prioridade")));

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("Status inexistente no corpo da requisição retorna 400")
    void statusInexistente() throws Exception {
        mockMvc.perform(patch("/api/chamados/TD-000010/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"FECHADO\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("GET de protocolo inexistente retorna 404")
    void protocoloInexistente() throws Exception {
        when(service.buscarPorProtocolo("TD-999999"))
                .thenThrow(new RecursoNaoEncontradoException("Chamado TD-999999 não encontrado."));

        mockMvc.perform(get("/api/chamados/TD-999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("Violação de regra de negócio retorna 422")
    void regraDeNegocioViolada() throws Exception {
        when(service.atualizarStatus(eq("TD-000010"), any()))
                .thenThrow(new RegraNegocioException("O chamado TD-000010 não está em atendimento."));

        mockMvc.perform(patch("/api/chamados/TD-000010/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"RESOLVIDO\"}"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.status").value(422));
    }
}