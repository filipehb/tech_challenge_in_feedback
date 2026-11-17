package com.filipearruda.relatorio;

import com.filipearruda.data.AvaliacaoRepository;
import com.filipearruda.domain.Avaliacao;
import com.filipearruda.domain.Urgencia;
import com.filipearruda.json.JsonMapper;
import com.microsoft.azure.functions.ExecutionContext;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.logging.Logger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RelatorioSemanalFunctionTest {

    @Test
    void publicaOJsonConsolidado() throws Exception {
        AvaliacaoRepository repository = mock(AvaliacaoRepository.class);
        ReportPublisher publisher = mock(ReportPublisher.class);
        ExecutionContext context = mock(ExecutionContext.class);
        when(context.getLogger()).thenReturn(Logger.getLogger("test"));
        when(repository.listSince(any())).thenReturn(List.of(
                new Avaliacao(1, "nota 1", 1, Urgencia.ALTA, Instant.parse("2026-11-16T15:00:00Z"))
        ));

        RelatorioSemanalFunction function = new RelatorioSemanalFunction(
                repository,
                publisher,
                JsonMapper.create(),
                Clock.fixed(Instant.parse("2026-11-17T12:00:00Z"), ZoneOffset.UTC)
        );

        function.run("{}", context);

        verify(repository).init();
        verify(publisher).publish(argThat(json -> json.contains("nota 1") && json.contains("ALTA") && json.contains("mediaNota")));
    }
}
