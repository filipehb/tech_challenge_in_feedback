package com.filipearruda.receber;

import com.filipearruda.data.AvaliacaoRepository;
import com.filipearruda.domain.Avaliacao;
import com.filipearruda.domain.Urgencia;
import com.filipearruda.json.JsonMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.OutputBinding;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReceberAvaliacaoFunctionTest {

    private AvaliacaoRepository repository;
    private ReceberAvaliacaoFunction function;
    private ExecutionContext context;
    private OutputBinding<String> fila;

    @BeforeEach
    void setUp() {
        repository = mock(AvaliacaoRepository.class);
        context = mock(ExecutionContext.class);
        when(context.getLogger()).thenReturn(Logger.getLogger("test"));
        function = new ReceberAvaliacaoFunction(JsonMapper.create(), repository);
        fila = mockFila();
    }

    @Test
    void getRespondeQueOServicoEstaNoAr() {
        HttpResponseMessage response = function.run(request(HttpMethod.GET, null), fila, context);
        assertEquals(HttpStatus.OK, response.getStatus());
        assertTrue(response.getBody().toString().contains("UP"));
    }

    @Test
    void notaCriticaVaiParaAFila() throws Exception {
        when(repository.save(any())).thenReturn(new Avaliacao(7, "nota 1", 1, Urgencia.ALTA, Instant.parse("2026-11-17T20:00:00Z")));

        HttpResponseMessage response = function.run(request(HttpMethod.POST, "{\"descricao\":\"nota 1\",\"nota\":1}"), fila, context);

        assertEquals(HttpStatus.CREATED, response.getStatus());
        assertTrue(response.getBody().toString().contains("\"enfileirada\":true"));
        verify(fila).setValue(anyString());
    }

    @Test
    void notaAltaNaoVaiParaAFila() throws Exception {
        when(repository.save(any())).thenReturn(new Avaliacao(8, "nota 9", 9, Urgencia.BAIXA, Instant.parse("2026-11-17T20:00:00Z")));

        HttpResponseMessage response = function.run(request(HttpMethod.POST, "{\"descricao\":\"nota 9\",\"nota\":9}"), fila, context);

        assertEquals(HttpStatus.CREATED, response.getStatus());
        assertTrue(response.getBody().toString().contains("\"enfileirada\":false"));
        verify(fila, never()).setValue(anyString());
    }

    @Test
    void corpoVazioERejeitado() {
        HttpResponseMessage response = function.run(request(HttpMethod.POST, " "), fila, context);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatus());
    }

    @Test
    void notaAusenteERejeitada() {
        HttpResponseMessage response = function.run(request(HttpMethod.POST, "{\"descricao\":\"sem nota\"}"), fila, context);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatus());
        assertTrue(response.getBody().toString().contains("nota"));
    }

    @Test
    void falhaDeBancoNaoPublicaNaFila() throws Exception {
        when(repository.save(any())).thenThrow(new SQLException("login failed"));

        HttpResponseMessage response = function.run(request(HttpMethod.POST, "{\"descricao\":\"nota 1\",\"nota\":1}"), fila, context);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatus());
        verify(fila, never()).setValue(anyString());
    }

    @SuppressWarnings("unchecked")
    private static OutputBinding<String> mockFila() {
        return mock(OutputBinding.class);
    }

    @SuppressWarnings("unchecked")
    private static HttpRequestMessage<Optional<String>> request(HttpMethod method, String body) {
        HttpRequestMessage<Optional<String>> request = mock(HttpRequestMessage.class);
        when(request.getHttpMethod()).thenReturn(method);
        when(request.getBody()).thenReturn(Optional.ofNullable(body));
        when(request.createResponseBuilder(any(HttpStatus.class))).thenAnswer(invocation -> {
            HttpStatus status = invocation.getArgument(0);
            return new HttpResponseMessage.Builder() {
                private Object responseBody;

                @Override
                public HttpResponseMessage.Builder status(com.microsoft.azure.functions.HttpStatusType httpStatus) {
                    return this;
                }

                @Override
                public HttpResponseMessage.Builder header(String key, String value) {
                    return this;
                }

                @Override
                public HttpResponseMessage.Builder body(Object body) {
                    this.responseBody = body;
                    return this;
                }

                @Override
                public HttpResponseMessage build() {
                    HttpResponseMessage response = mock(HttpResponseMessage.class);
                    when(response.getStatus()).thenReturn(status);
                    when(response.getBody()).thenReturn(responseBody);
                    return response;
                }
            };
        });
        return request;
    }
}
