package com.filipearruda.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AvaliacaoCriticaMessage(String descricao, int nota, String urgencia, Instant dataEnvio) {
}
