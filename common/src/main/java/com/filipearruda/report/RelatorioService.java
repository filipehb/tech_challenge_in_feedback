package com.filipearruda.report;

import com.filipearruda.domain.Avaliacao;
import com.filipearruda.domain.Urgencia;
import com.filipearruda.dto.RelatorioSemanal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class RelatorioService {
    private RelatorioService() {
    }

    public static RelatorioSemanal consolidar(List<Avaliacao> avaliacoes, Instant inicio, Instant fim, Instant geradoEm, ZoneId zone) {
        List<Avaliacao> ordenadas = avaliacoes.stream()
                .sorted(Comparator.comparing(Avaliacao::dataEnvio))
                .toList();

        Map<LocalDate, List<Avaliacao>> agrupadasPorDia = ordenadas.stream()
                .collect(Collectors.groupingBy(avaliacao -> avaliacao.dataEnvio().atZone(zone).toLocalDate()));

        List<RelatorioSemanal.QuantidadePorDia> porDia = agrupadasPorDia.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new RelatorioSemanal.QuantidadePorDia(
                        entry.getKey().toString(),
                        entry.getValue().size(),
                        media(entry.getValue())
                ))
                .toList();

        List<RelatorioSemanal.QuantidadePorUrgencia> porUrgencia = Arrays.stream(Urgencia.values())
                .map(urgencia -> {
                    List<Avaliacao> doTipo = ordenadas.stream()
                            .filter(avaliacao -> avaliacao.urgencia() == urgencia)
                            .toList();
                    return new RelatorioSemanal.QuantidadePorUrgencia(urgencia.name(), doTipo.size(), media(doTipo));
                })
                .toList();

        List<RelatorioSemanal.Item> itens = ordenadas.stream()
                .map(avaliacao -> new RelatorioSemanal.Item(
                        avaliacao.descricao(),
                        avaliacao.urgencia().name(),
                        avaliacao.dataEnvio(),
                        avaliacao.nota()
                ))
                .toList();

        return new RelatorioSemanal(geradoEm, inicio, fim, ordenadas.size(), media(ordenadas), porDia, porUrgencia, itens);
    }

    private static Double media(List<Avaliacao> avaliacoes) {
        if (avaliacoes.isEmpty()) {
            return null;
        }
        double average = avaliacoes.stream().mapToInt(Avaliacao::nota).average().orElseThrow();
        return BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
