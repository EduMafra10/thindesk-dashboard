package br.com.thindesk.dashboard.config;

import br.com.thindesk.dashboard.model.Chamado;
import br.com.thindesk.dashboard.model.Prioridade;
import br.com.thindesk.dashboard.model.TipoChamado;
import br.com.thindesk.dashboard.repository.ChamadoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Component
@ConditionalOnProperty(name = "thindesk.carga-inicial.habilitada", havingValue = "true")
public class CargaInicialDados implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CargaInicialDados.class);

    private static final int QUANTIDADE_CHAMADOS = 320;
    private static final int DIAS_DE_HISTORICO = 180;

    private static final List<String> TECNICOS = List.of(
            "Carlos Mendes", "Juliana Prado", "Rafael Lima", "Beatriz Souza", "Diego Martins");

    private static final List<String> SETORES = List.of(
            "Financeiro", "Recursos Humanos", "Comercial", "Logística", "Jurídico", "Marketing", "Atendimento");

    private static final List<String> SOLICITANTES = List.of(
            "Ana Ribeiro", "Bruno Carvalho", "Camila Torres", "Daniel Freitas", "Fernanda Alves",
            "Gustavo Rocha", "Larissa Nunes", "Marcos Pereira", "Patrícia Gomes", "Thiago Barros");

    private static final Map<TipoChamado, List<String>> TITULOS = Map.of(
            TipoChamado.HARDWARE, List.of("Computador não liga", "Monitor piscando", "Teclado com teclas falhando"),
            TipoChamado.SOFTWARE, List.of("Erro ao abrir o Excel", "Instalação do Adobe Reader", "Atualização do Windows travada"),
            TipoChamado.REDE, List.of("Sem acesso à internet", "Wi-Fi caindo constantemente", "Lentidão na rede"),
            TipoChamado.ACESSO, List.of("Senha expirada", "Usuário bloqueado", "Acesso à pasta compartilhada"),
            TipoChamado.IMPRESSORA, List.of("Impressora não imprime", "Papel atolado", "Instalação de impressora de rede"),
            TipoChamado.EMAIL, List.of("Caixa de e-mail cheia", "Não recebe e-mails externos", "Configuração do Outlook"),
            TipoChamado.SISTEMA_INTERNO, List.of("Erro ao emitir nota no ERP", "Relatório do ERP não carrega", "Falha de login no sistema de ponto"));

    private static final Map<TipoChamado, List<String>> SOLUCOES = Map.of(
            TipoChamado.HARDWARE, List.of("Fonte substituída e equipamento testado com o usuário.", "Cabo de vídeo trocado e monitor recalibrado."),
            TipoChamado.SOFTWARE, List.of("Aplicativo reparado pelo painel de controle.", "Software reinstalado e licença validada."),
            TipoChamado.REDE, List.of("Switch do setor reiniciado e link normalizado.", "Driver da placa de rede atualizado."),
            TipoChamado.ACESSO, List.of("Senha redefinida e usuário orientado sobre a política de senhas.", "Permissão concedida após aprovação do gestor."),
            TipoChamado.IMPRESSORA, List.of("Fila de impressão limpa e spooler reiniciado.", "Papel removido e rolete limpo."),
            TipoChamado.EMAIL, List.of("Arquivamento automático configurado no Outlook.", "Regra de spam ajustada no servidor de e-mail."),
            TipoChamado.SISTEMA_INTERNO, List.of("Cache do sistema limpo e sessão reiniciada.", "Chamado escalado ao fornecedor e correção aplicada."));

    private static final List<TipoChamado> DISTRIBUICAO_TIPOS = distribuir(Map.of(
            TipoChamado.ACESSO, 26, TipoChamado.SOFTWARE, 20, TipoChamado.IMPRESSORA, 16,
            TipoChamado.REDE, 14, TipoChamado.HARDWARE, 10, TipoChamado.EMAIL, 8,
            TipoChamado.SISTEMA_INTERNO, 6));

    private static final List<Prioridade> DISTRIBUICAO_PRIORIDADES = distribuir(Map.of(
            Prioridade.BAIXA, 30, Prioridade.MEDIA, 40, Prioridade.ALTA, 22, Prioridade.CRITICA, 8));

    private final ChamadoRepository repository;
    private final RandomGenerator random = RandomGenerator.getDefault();

    public CargaInicialDados(ChamadoRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            log.info("Carga inicial ignorada: a coleção de chamados já possui registros.");
            return;
        }

        LocalDateTime agora = LocalDateTime.now();
        List<LocalDateTime> aberturas = Stream.generate(() -> gerarDataAbertura(agora))
                .limit(QUANTIDADE_CHAMADOS)
                .sorted()
                .toList();

        List<Chamado> chamados = IntStream.range(0, aberturas.size())
                .mapToObj(i -> criarChamado(i + 1, aberturas.get(i), agora))
                .toList();

        repository.saveAll(chamados);
        log.info("Carga inicial concluída: {} chamados cadastrados.", chamados.size());
    }

    private Chamado criarChamado(int sequencial, LocalDateTime abertura, LocalDateTime agora) {
        TipoChamado tipo = sortear(DISTRIBUICAO_TIPOS);
        String titulo = sortear(TITULOS.get(tipo));

        Chamado chamado = new Chamado(
                "TD-%06d".formatted(sequencial),
                titulo,
                "Solicitação registrada pelo portal: " + titulo.toLowerCase() + ".",
                tipo,
                sortear(DISTRIBUICAO_PRIORIDADES),
                sortear(SOLICITANTES),
                sortear(SETORES),
                abertura);

        simularAtendimento(chamado, agora);
        return chamado;
    }

    private void simularAtendimento(Chamado chamado, LocalDateTime agora) {
        LocalDateTime abertura = chamado.getDataAbertura();
        boolean recente = abertura.isAfter(agora.minusDays(30));

        if (recente && chance(15)) {
            return;
        }

        LocalDateTime inicio = minimo(abertura.plusMinutes(entre(10, 240)), agora);
        chamado.iniciarAtendimento(sortear(TECNICOS), inicio);

        LocalDateTime conclusao = inicio.plusMinutes(entre(30, 4320));
        if (conclusao.isAfter(agora) || (recente && chance(20))) {
            if (chance(35)) {
                chamado.aguardarUsuario(minimo(inicio.plusMinutes(30), agora));
            }
            return;
        }

        if (chance(6)) {
            chamado.cancelar("Cancelado a pedido do solicitante.", conclusao);
        } else {
            chamado.resolver(sortear(SOLUCOES.get(chamado.getTipo())), conclusao);
        }
    }

    private LocalDateTime gerarDataAbertura(LocalDateTime agora) {
        LocalDateTime data = agora.minusDays(random.nextInt(DIAS_DE_HISTORICO))
                .truncatedTo(ChronoUnit.MINUTES)
                .withHour(entre(8, 18))
                .withMinute(random.nextInt(60));
        return data.isAfter(agora) ? agora.minusHours(1) : data;
    }

    private <T> T sortear(List<T> opcoes) {
        return opcoes.get(random.nextInt(opcoes.size()));
    }

    private int entre(int minimo, int maximo) {
        return random.nextInt(minimo, maximo + 1);
    }

    private boolean chance(int percentual) {
        return random.nextInt(100) < percentual;
    }

    private static LocalDateTime minimo(LocalDateTime a, LocalDateTime b) {
        return a.isBefore(b) ? a : b;
    }

    private static <T> List<T> distribuir(Map<T, Integer> pesos) {
        List<T> resultado = new ArrayList<>();
        pesos.forEach((item, peso) -> resultado.addAll(Collections.nCopies(peso, item)));
        return List.copyOf(resultado);
    }
}