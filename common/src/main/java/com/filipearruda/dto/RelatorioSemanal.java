package com.filipearruda.dto;

import java.time.Instant;
import java.util.List;

public record RelatorioSemanal(
        Instant geradoEm,
        Instant periodoInicio,
        Instant periodoFim,
        long total,
        Double mediaNota,
        List<QuantidadePorDia> porDia,
        List<QuantidadePorUrgencia> porUrgencia,
        List<Item> avaliacoes
) {
    public record QuantidadePorDia(String dia, long quantidade, Double media) {
    }

    public record QuantidadePorUrgencia(String urgencia, long quantidade, Double media) {
    }

    public record Item(String descricao, String urgencia, Instant dataEnvio, int nota) {
    }
}
