package com.filipearruda.report;

import com.filipearruda.domain.Avaliacao;
import com.filipearruda.domain.Urgencia;
import com.filipearruda.dto.RelatorioSemanal;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RelatorioServiceTest {

    private static final ZoneId BRASILIA = ZoneId.of("America/Sao_Paulo");

    @Test
    void consolidaMediaQuantidadePorDiaEPorUrgencia() {
        List<Avaliacao> avaliacoes = List.of(
                new Avaliacao(1, "ruim", 2, Urgencia.ALTA, Instant.parse("2026-11-16T15:00:00Z")),
                new Avaliacao(2, "ótimo", 9, Urgencia.BAIXA, Instant.parse("2026-11-16T18:00:00Z")),
                new Avaliacao(3, "ok", 5, Urgencia.MEDIA, Instant.parse("2026-11-17T15:00:00Z"))
        );

        RelatorioSemanal relatorio = RelatorioService.consolidar(
                avaliacoes,
                Instant.parse("2026-11-10T15:00:00Z"),
                Instant.parse("2026-11-17T15:00:00Z"),
                Instant.parse("2026-11-17T15:00:00Z"),
                BRASILIA
        );

        assertEquals(3, relatorio.total());
        assertEquals(5.33, relatorio.mediaNota());
        assertEquals(2, relatorio.porDia().size());
        assertEquals("2026-11-16", relatorio.porDia().get(0).dia());
        assertEquals(2, relatorio.porDia().get(0).quantidade());
        assertEquals(5.5, relatorio.porDia().get(0).media());
        assertEquals("2026-11-17", relatorio.porDia().get(1).dia());
        assertEquals(1, relatorio.porDia().get(1).quantidade());
        assertEquals(List.of("ALTA", "MEDIA", "BAIXA"), relatorio.porUrgencia().stream().map(RelatorioSemanal.QuantidadePorUrgencia::urgencia).toList());
        assertEquals(1, relatorio.porUrgencia().get(0).quantidade());
        assertEquals(3, relatorio.avaliacoes().size());
        assertEquals("ruim", relatorio.avaliacoes().get(0).descricao());
    }

    @Test
    void semanaVaziaNaoInventaMedia() {
        RelatorioSemanal relatorio = RelatorioService.consolidar(
                List.of(),
                Instant.parse("2026-11-10T00:00:00Z"),
                Instant.parse("2026-11-17T00:00:00Z"),
                Instant.parse("2026-11-17T00:00:00Z"),
                BRASILIA
        );

        assertEquals(0, relatorio.total());
        assertNull(relatorio.mediaNota());
        assertEquals(0, relatorio.porDia().size());
        assertEquals(3, relatorio.porUrgencia().size());
        assertEquals(0, relatorio.porUrgencia().get(0).quantidade());
        assertNull(relatorio.porUrgencia().get(0).media());
    }
}
