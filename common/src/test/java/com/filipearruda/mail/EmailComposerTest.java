package com.filipearruda.mail;

import com.filipearruda.dto.AvaliacaoCriticaMessage;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailComposerTest {

    @Test
    void incluiDescricaoUrgenciaEDataDeEnvio() {
        AvaliacaoCriticaMessage message = new AvaliacaoCriticaMessage(
                "nota 1",
                1,
                "ALTA",
                Instant.parse("2026-11-17T20:00:00Z")
        );

        String subject = EmailComposer.subject(message);
        String body = EmailComposer.body(message);

        assertTrue(subject.contains("ALTA"));
        assertTrue(body.contains("Descrição: nota 1"));
        assertTrue(body.contains("Urgência: ALTA"));
        assertTrue(body.contains("Data de envio: 17/11/2026 17:00"));
    }
}
