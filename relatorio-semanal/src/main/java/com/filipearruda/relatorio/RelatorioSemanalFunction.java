package com.filipearruda.relatorio;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.filipearruda.data.AvaliacaoRepository;
import com.filipearruda.domain.Avaliacao;
import com.filipearruda.dto.RelatorioSemanal;
import com.filipearruda.json.JsonMapper;
import com.filipearruda.report.RelatorioService;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.TimerTrigger;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

public class RelatorioSemanalFunction {
    private static final ZoneId BRASILIA = ZoneId.of("America/Sao_Paulo");

    private final AvaliacaoRepository repository;
    private final ReportPublisher publisher;
    private final ObjectMapper mapper;
    private final Clock clock;

    public RelatorioSemanalFunction() {
        this(new AvaliacaoRepository(), new BlobReportPublisher(), JsonMapper.create(), Clock.systemUTC());
    }

    public RelatorioSemanalFunction(AvaliacaoRepository repository, ReportPublisher publisher, ObjectMapper mapper, Clock clock) {
        this.repository = repository;
        this.publisher = publisher;
        this.mapper = mapper;
        this.clock = clock;
    }

    @FunctionName("fn-relatorio-semanal")
    public void run(
            @TimerTrigger(name = "timer", schedule = "0 0 8 * * 1")
            String timerInfo,
            final ExecutionContext context) throws Exception {
        Instant fim = clock.instant();
        Instant inicio = fim.minus(Duration.ofDays(7));
        repository.init();
        List<Avaliacao> avaliacoes = repository.listSince(inicio);
        RelatorioSemanal relatorio = RelatorioService.consolidar(avaliacoes, inicio, fim, fim, BRASILIA);
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(relatorio);
        context.getLogger().info(json);
        publisher.publish(json);
        context.getLogger().info("Relatório semanal publicado. Total=" + relatorio.total() + " média=" + relatorio.mediaNota());
    }
}
