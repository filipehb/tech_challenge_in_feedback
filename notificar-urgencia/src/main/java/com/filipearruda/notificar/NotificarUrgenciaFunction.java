package com.filipearruda.notificar;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.filipearruda.dto.AvaliacaoCriticaMessage;
import com.filipearruda.json.JsonMapper;
import com.filipearruda.mail.AzureEmailNotifier;
import com.filipearruda.mail.EmailNotifier;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.QueueTrigger;

public class NotificarUrgenciaFunction {
    private final ObjectMapper mapper;
    private final EmailNotifier emailNotifier;

    public NotificarUrgenciaFunction() {
        this(JsonMapper.create(), new AzureEmailNotifier());
    }

    public NotificarUrgenciaFunction(ObjectMapper mapper, EmailNotifier emailNotifier) {
        this.mapper = mapper;
        this.emailNotifier = emailNotifier;
    }

    @FunctionName("fn-notificar-urgencia")
    public void run(
            @QueueTrigger(name = "message", queueName = "avaliacoes-criticas", connection = "AzureWebJobsStorage")
            String message,
            final ExecutionContext context) {
        AvaliacaoCriticaMessage critica;
        try {
            critica = mapper.readValue(message, AvaliacaoCriticaMessage.class);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Mensagem da fila inválida.", exception);
        }
        if (critica.descricao() == null || critica.descricao().isBlank() || critica.urgencia() == null || critica.dataEnvio() == null) {
            throw new IllegalArgumentException("A mensagem da fila não contém descrição, urgência e data de envio.");
        }

        context.getLogger().info("Enviando e-mail de urgência " + critica.urgencia() + ".");
        emailNotifier.send(critica);
        context.getLogger().info("E-mail de urgência enviado.");
    }
}
