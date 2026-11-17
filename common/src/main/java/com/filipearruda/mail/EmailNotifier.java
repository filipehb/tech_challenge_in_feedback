package com.filipearruda.mail;

import com.filipearruda.dto.AvaliacaoCriticaMessage;

public interface EmailNotifier {
    void send(AvaliacaoCriticaMessage message);
}
