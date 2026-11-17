package com.filipearruda.receber;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.filipearruda.data.AvaliacaoRepository;
import com.filipearruda.domain.Avaliacao;
import com.filipearruda.domain.Urgencia;
import com.filipearruda.dto.AvaliacaoCriticaMessage;
import com.filipearruda.dto.AvaliacaoRequest;
import com.filipearruda.json.JsonMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.OutputBinding;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.microsoft.azure.functions.annotation.QueueOutput;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;

public class ReceberAvaliacaoFunction {
    private final ObjectMapper mapper;
    private final AvaliacaoRepository repository;

    public ReceberAvaliacaoFunction() {
        this(JsonMapper.create(), new AvaliacaoRepository());
    }

    public ReceberAvaliacaoFunction(ObjectMapper mapper, AvaliacaoRepository repository) {
        this.mapper = mapper;
        this.repository = repository;
    }

    @FunctionName("fn-receber-avaliacao")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {HttpMethod.GET, HttpMethod.POST}, authLevel = com.microsoft.azure.functions.annotation.AuthorizationLevel.ANONYMOUS, route = "avaliacao")
            HttpRequestMessage<Optional<String>> request,
            @QueueOutput(name = "filaCritica", queueName = "avaliacoes-criticas", connection = "AzureWebJobsStorage")
            OutputBinding<String> filaCritica,
            final ExecutionContext context) {

        if (request.getHttpMethod() == HttpMethod.GET) {
            return json(request, HttpStatus.OK, "{\"status\":\"UP\",\"service\":\"fn-receber-avaliacao\"}");
        }

        String body = request.getBody().orElse("");
        if (body.isBlank()) {
            return json(request, HttpStatus.BAD_REQUEST, "{\"erro\":\"O corpo da requisição não pode ser vazio.\"}");
        }

        AvaliacaoRequest payload;
        try {
            payload = mapper.readValue(body, AvaliacaoRequest.class);
        } catch (Exception exception) {
            context.getLogger().warning("JSON inválido: " + exception.getMessage());
            return json(request, HttpStatus.BAD_REQUEST, "{\"erro\":\"JSON inválido.\"}");
        }

        if (payload.getDescricao() == null || payload.getDescricao().isBlank()) {
            return json(request, HttpStatus.BAD_REQUEST, "{\"erro\":\"'descricao' é obrigatória.\"}");
        }
        if (payload.getNota() == null || payload.getNota() < 0 || payload.getNota() > 10) {
            return json(request, HttpStatus.BAD_REQUEST, "{\"erro\":\"'nota' é obrigatória e deve estar entre 0 e 10.\"}");
        }

        Urgencia urgencia = Urgencia.fromNota(payload.getNota());
        Instant dataEnvio = Instant.now();
        try {
            repository.init();
            Avaliacao salva = repository.save(new Avaliacao(0, payload.getDescricao().trim(), payload.getNota(), urgencia, dataEnvio));
            boolean enfileirada = false;
            if (urgencia.isCritica()) {
                AvaliacaoCriticaMessage message = new AvaliacaoCriticaMessage(
                        salva.descricao(), salva.nota(), salva.urgencia().name(), salva.dataEnvio());
                filaCritica.setValue(mapper.writeValueAsString(message));
                enfileirada = true;
                context.getLogger().info("Avaliação crítica " + salva.id() + " enviada para a fila avaliacoes-criticas.");
            }
            String response = mapper.createObjectNode()
                    .put("mensagem", "Avaliação registrada.")
                    .put("id", salva.id())
                    .put("urgencia", salva.urgencia().name())
                    .put("enfileirada", enfileirada)
                    .toString();
            return json(request, HttpStatus.CREATED, response);
        } catch (SQLException exception) {
            context.getLogger().severe("Falha ao gravar avaliação: " + exception.getMessage());
            return json(request, HttpStatus.INTERNAL_SERVER_ERROR, "{\"erro\":\"Falha ao gravar a avaliação.\"}");
        } catch (Exception exception) {
            context.getLogger().severe("Falha ao publicar avaliação crítica: " + exception.getMessage());
            return json(request, HttpStatus.INTERNAL_SERVER_ERROR, "{\"erro\":\"A avaliação foi recebida, mas não pôde ser publicada na fila.\"}");
        }
    }

    private static HttpResponseMessage json(HttpRequestMessage<?> request, HttpStatus status, String body) {
        return request.createResponseBuilder(status)
                .header("Content-Type", "application/json")
                .body(body)
                .build();
    }
}
