package com.filipearruda.mail;

import com.filipearruda.dto.AvaliacaoCriticaMessage;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class EmailComposer {
    private static final ZoneId BRASILIA = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(BRASILIA);

    private EmailComposer() {
    }

    public static String subject(AvaliacaoCriticaMessage message) {
        return "Avaliação crítica: " + message.urgencia();
    }

    public static String body(AvaliacaoCriticaMessage message) {
        return """
                Uma avaliação crítica foi recebida.

                Descrição: %s
                Urgência: %s
                Data de envio: %s (horário de Brasília)
                Nota: %d
                """.formatted(
                message.descricao(),
                message.urgencia(),
                DATA.format(message.dataEnvio()),
                message.nota()
        );
    }
}
