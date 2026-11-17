package com.filipearruda.domain;

import java.time.Instant;

public record Avaliacao(long id, String descricao, int nota, Urgencia urgencia, Instant dataEnvio) {
}
