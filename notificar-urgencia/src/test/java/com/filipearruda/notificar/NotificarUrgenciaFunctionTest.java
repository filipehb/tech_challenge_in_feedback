package com.filipearruda.notificar;

import com.filipearruda.dto.AvaliacaoCriticaMessage;
import com.filipearruda.json.JsonMapper;
import com.filipearruda.mail.EmailNotifier;
import com.microsoft.azure.functions.ExecutionContext;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificarUrgenciaFunctionTest {

    @Test
    void enviaEmailComOsDadosDaMensagem() {
        EmailNotifier email = mock(EmailNotifier.class);
        ExecutionContext context = mock(ExecutionContext.class);
        when(context.getLogger()).thenReturn(Logger.getLogger("test"));
        NotificarUrgenciaFunction function = new NotificarUrgenciaFunction(JsonMapper.create(), email);

        function.run("""
                {"descricao":"nota 1","nota":1,"urgencia":"ALTA","dataEnvio":"2026-11-17T20:00:00Z"}
                """, context);

        verify(email).send(argThat((AvaliacaoCriticaMessage message) ->
                message.descricao().equals("nota 1")
                        && message.urgencia().equals("ALTA")
                        && message.nota() == 1));
    }

    @Test
    void mensagemInvalidaNaoEEngolida() {
        EmailNotifier email = mock(EmailNotifier.class);
        ExecutionContext context = mock(ExecutionContext.class);
        when(context.getLogger()).thenReturn(Logger.getLogger("test"));
        NotificarUrgenciaFunction function = new NotificarUrgenciaFunction(JsonMapper.create(), email);

        assertThrows(IllegalArgumentException.class, () -> function.run("nao-e-json", context));
    }
}
