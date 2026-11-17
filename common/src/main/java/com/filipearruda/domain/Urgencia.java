package com.filipearruda.domain;

public enum Urgencia {
    ALTA,
    MEDIA,
    BAIXA;

    public static Urgencia fromNota(int nota) {
        if (nota < 0 || nota > 10) {
            throw new IllegalArgumentException("Nota fora do intervalo 0 a 10.");
        }
        if (nota <= 3) {
            return ALTA;
        }
        if (nota <= 6) {
            return MEDIA;
        }
        return BAIXA;
    }

    public boolean isCritica() {
        return this == ALTA;
    }
}
