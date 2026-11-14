package br.com.thindesk.dashboard.service;

import br.com.thindesk.dashboard.dto.Agrupamento;
import br.com.thindesk.dashboard.dto.ChamadoFiltro;
import br.com.thindesk.dashboard.dto.ContagemStatusResponse;
import br.com.thindesk.dashboard.dto.IndicadoresResponse;
import br.com.thindesk.dashboard.dto.OpcoesFiltroResponse;
import br.com.thindesk.dashboard.dto.PontoSerieResponse;
import br.com.thindesk.dashboard.dto.RankingTipoResponse;
import br.com.thindesk.dashboard.exception.RegraNegocioException;
import br.com.thindesk.dashboard.model.Chamado;
import br.com.thindesk.dashboard.model.Prioridade;
import br.com.thindesk.dashboard.model.StatusChamado;
import br.com.thindesk.dashboard.model.TipoChamado;
import br.com.thindesk.dashboard.repository.ChamadoCriteria;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.ArithmeticOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Service
public class DashboardService {

    private static final int LIMITE_PONTOS_SERIE = 400;
    private static final double MILISSEGUNDOS_POR_HORA = 3_600_000d;
    private static final List<StatusChamado> STATUS_PENDENTES =
            List.of(StatusChamado.ABERTO, StatusChamado.EM_ANDAMENTO, StatusChamado.AGUARDANDO_USUARIO);

    private final MongoTemplate mongoTemplate;

    public DashboardService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public IndicadoresResponse indicadores(ChamadoFiltro filtro) {
        Criteria criteria = ChamadoCriteria.aPartirDe(filtro);
        Map<StatusChamado, Long> porStatus = contarPorStatus(criteria);
        long total = somar(porStatus);
        long resolvidos = porStatus.get(StatusChamado.RESOLVIDO);

        return new IndicadoresResponse(
                total,
                porStatus.get(StatusChamado.ABERTO),
                porStatus.get(StatusChamado.EM_ANDAMENTO),
                porStatus.get(StatusChamado.AGUARDANDO_USUARIO),
                resolvidos,
                porStatus.get(StatusChamado.CANCELADO),
                percentual(resolvidos, total),
                tempoMedioResolucaoHoras(criteria),
                contarPendentesAltaPrioridade(criteria));
    }

    public List<ContagemStatusResponse> situacao(ChamadoFiltro filtro) {
        Map<StatusChamado, Long> porStatus = contarPorStatus(ChamadoCriteria.aPartirDe(filtro));
        long total = somar(porStatus);

        return porStatus.entrySet().stream()
                .map(item -> new ContagemStatusResponse(
                        item.getKey(),
                        item.getKey().getDescricao(),
                        item.getValue(),
                        percentual(item.getValue(), total)))
                .toList();
    }

    public List<RankingTipoResponse> tiposMaisAtendidos(ChamadoFiltro filtro, int limite) {
        Criteria atendidos = new Criteria().andOperator(
                ChamadoCriteria.aPartirDe(filtro),
                Criteria.where("dataInicioAtendimento").ne(null));

        List<Document> resultado = agregar(atendidos,
                Aggregation.group("tipo").count().as("quantidade"),
                Aggregation.sort(Sort.by(Sort.Direction.DESC, "quantidade")));
        long total = resultado.stream().mapToLong(DashboardService::quantidade).sum();

        return IntStream.range(0, Math.min(limite, resultado.size()))
                .mapToObj(indice -> {
                    Document documento = resultado.get(indice);
                    TipoChamado tipo = TipoChamado.valueOf(documento.getString("_id"));
                    long quantidade = quantidade(documento);
                    return new RankingTipoResponse(indice + 1, tipo, tipo.getDescricao(),
                            quantidade, percentual(quantidade, total));
                })
                .toList();
    }

    public List<PontoSerieResponse> atendimentosPorPeriodo(ChamadoFiltro filtro, Agrupamento agrupamento) {
        Query consulta = Query.query(ChamadoCriteria.aPartirDe(filtro));
        consulta.fields().include("status", "dataAbertura", "dataConclusao");
        List<Chamado> chamados = mongoTemplate.find(consulta, Chamado.class);

        if (chamados.isEmpty()) {
            return List.of();
        }

        LocalDate inicio = filtro.dataInicio() != null
                ? filtro.dataInicio()
                : chamados.stream()
                        .map(chamado -> chamado.getDataAbertura().toLocalDate())
                        .min(Comparator.naturalOrder())
                        .orElseThrow();
        LocalDate fim = filtro.dataFim() != null ? filtro.dataFim() : LocalDate.now();

        Map<LocalDate, Long> abertos = agruparPorPeriodo(
                chamados.stream().map(Chamado::getDataAbertura), agrupamento);
        Map<LocalDate, Long> resolvidos = agruparPorPeriodo(
                chamados.stream()
                        .filter(chamado -> chamado.getStatus() == StatusChamado.RESOLVIDO)
                        .map(Chamado::getDataConclusao),
                agrupamento);

        List<PontoSerieResponse> serie = new ArrayList<>();
        for (LocalDate periodo = agrupamento.inicioDoPeriodo(inicio);
             !periodo.isAfter(fim);
             periodo = agrupamento.proximoPeriodo(periodo)) {

            if (serie.size() == LIMITE_PONTOS_SERIE) {
                throw new RegraNegocioException(
                        "Período muito longo para o agrupamento selecionado. Reduza o período ou use um agrupamento maior.");
            }
            serie.add(new PontoSerieResponse(
                    periodo,
                    agrupamento.rotulo(periodo),
                    abertos.getOrDefault(periodo, 0L),
                    resolvidos.getOrDefault(periodo, 0L)));
        }
        return serie;
    }

    public OpcoesFiltroResponse opcoesFiltro() {
        List<String> tecnicos = mongoTemplate
                .findDistinct(new Query(), "tecnicoResponsavel", Chamado.class, String.class)
                .stream()
                .filter(Objects::nonNull)
                .sorted()
                .toList();

        return new OpcoesFiltroResponse(
                opcoes(StatusChamado.values(), StatusChamado::getDescricao),
                opcoes(TipoChamado.values(), TipoChamado::getDescricao),
                opcoes(Prioridade.values(), Prioridade::getDescricao),
                opcoes(Agrupamento.values(), Agrupamento::getDescricao),
                tecnicos);
    }

    private Map<StatusChamado, Long> contarPorStatus(Criteria criteria) {
        Map<StatusChamado, Long> contagem = new EnumMap<>(StatusChamado.class);
        Arrays.stream(StatusChamado.values()).forEach(status -> contagem.put(status, 0L));

        agregar(criteria, Aggregation.group("status").count().as("quantidade"))
                .forEach(documento -> contagem.put(
                        StatusChamado.valueOf(documento.getString("_id")),
                        quantidade(documento)));
        return contagem;
    }

    private Double tempoMedioResolucaoHoras(Criteria criteria) {
        Criteria resolvidos = new Criteria().andOperator(
                criteria,
                Criteria.where("status").is(StatusChamado.RESOLVIDO));

        return agregar(resolvidos,
                Aggregation.project()
                        .and(ArithmeticOperators.Subtract.valueOf("dataConclusao").subtract("dataAbertura"))
                        .as("duracao"),
                Aggregation.group().avg("duracao").as("media"))
                .stream()
                .findFirst()
                .map(documento -> arredondar(((Number) documento.get("media")).doubleValue() / MILISSEGUNDOS_POR_HORA))
                .orElse(null);
    }

    private long contarPendentesAltaPrioridade(Criteria criteria) {
        Criteria pendentes = new Criteria().andOperator(
                criteria,
                Criteria.where("status").in(STATUS_PENDENTES),
                Criteria.where("prioridade").in(Prioridade.ALTA, Prioridade.CRITICA));
        return mongoTemplate.count(Query.query(pendentes), Chamado.class);
    }

    private List<Document> agregar(Criteria criteria, AggregationOperation... operacoes) {
        List<AggregationOperation> etapas = new ArrayList<>();
        etapas.add(Aggregation.match(criteria));
        etapas.addAll(List.of(operacoes));
        return mongoTemplate
                .aggregate(Aggregation.newAggregation(Chamado.class, etapas), Document.class)
                .getMappedResults();
    }

    private static Map<LocalDate, Long> agruparPorPeriodo(Stream<LocalDateTime> datas, Agrupamento agrupamento) {
        return datas.filter(Objects::nonNull)
                .collect(Collectors.groupingBy(
                        data -> agrupamento.inicioDoPeriodo(data.toLocalDate()),
                        Collectors.counting()));
    }

    private static <E extends Enum<E>> List<OpcoesFiltroResponse.Opcao> opcoes(
            E[] valores, Function<E, String> descricao) {
        return Arrays.stream(valores)
                .map(valor -> new OpcoesFiltroResponse.Opcao(valor.name(), descricao.apply(valor)))
                .toList();
    }

    private static long somar(Map<StatusChamado, Long> contagem) {
        return contagem.values().stream().mapToLong(Long::longValue).sum();
    }

    private static long quantidade(Document documento) {
        return ((Number) documento.get("quantidade")).longValue();
    }

    private static double percentual(long parte, long total) {
        return total == 0 ? 0 : arredondar(parte * 100.0 / total);
    }

    private static double arredondar(double valor) {
        return Math.round(valor * 10) / 10.0;
    }
}