package com.filipearruda.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UrgenciaTest {

    @Test
    void classificaOsLimites() {
        assertEquals(Urgencia.ALTA, Urgencia.fromNota(0));
        assertEquals(Urgencia.ALTA, Urgencia.fromNota(3));
        assertEquals(Urgencia.MEDIA, Urgencia.fromNota(4));
        assertEquals(Urgencia.MEDIA, Urgencia.fromNota(6));
        assertEquals(Urgencia.BAIXA, Urgencia.fromNota(7));
        assertEquals(Urgencia.BAIXA, Urgencia.fromNota(10));
    }

    @Test
    void somenteAltaECritica() {
        assertTrue(Urgencia.ALTA.isCritica());
        assertFalse(Urgencia.MEDIA.isCritica());
        assertFalse(Urgencia.BAIXA.isCritica());
    }

    @Test
    void rejeitaNotaForaDoIntervalo() {
        assertThrows(IllegalArgumentException.class, () -> Urgencia.fromNota(-1));
        assertThrows(IllegalArgumentException.class, () -> Urgencia.fromNota(11));
    }
}
