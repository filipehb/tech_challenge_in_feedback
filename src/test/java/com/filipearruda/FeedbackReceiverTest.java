package com.filipearruda;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.filipearruda.data.DatabaseService;
import com.filipearruda.dto.FeedbackRequest;
import com.filipearruda.request.FeedbackReceiver;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FeedbackReceiverTest {

    private DatabaseService mockDatabaseService;
    private FeedbackReceiver receiver;
    private ExecutionContext mockContext;

    @BeforeEach
    void setUp() {
        mockDatabaseService = mock(DatabaseService.class);
        mockContext = mock(ExecutionContext.class);
        when(mockContext.getLogger()).thenReturn(Logger.getLogger("TestLogger"));
        receiver = new FeedbackReceiver(new ObjectMapper(), mockDatabaseService);
    }

    @SuppressWarnings("unchecked")
    private HttpRequestMessage<Optional<String>> createMockRequest(HttpMethod method, String body) {
        HttpRequestMessage<Optional<String>> request = mock(HttpRequestMessage.class);
        when(request.getHttpMethod()).thenReturn(method);
        when(request.getBody()).thenReturn(Optional.ofNullable(body));

        when(request.createResponseBuilder(any(HttpStatus.class))).thenAnswer(invocation -> {
            HttpStatus status = invocation.getArgument(0);
            return new HttpResponseMessage.Builder() {
                private Object responseBody;
                private final Map<String, String> headers = new HashMap<>();

                @Override
                public HttpResponseMessage.Builder status(com.microsoft.azure.functions.HttpStatusType httpStatus) {
                    return this;
                }

                @Override
                public HttpResponseMessage.Builder header(String key, String value) {
                    headers.put(key, value);
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
                    when(response.getStatusCode()).thenReturn(status.value());
                    when(response.getBody()).thenReturn(responseBody);
                    return response;
                }
            };
        });

        return request;
    }

    @Test
    void testGetHealthCheck() {
        HttpRequestMessage<Optional<String>> request = createMockRequest(HttpMethod.GET, null);
        HttpResponseMessage response = receiver.run(request, mockContext);

        assertEquals(HttpStatus.OK, response.getStatus());
        assertTrue(response.getBody().toString().contains("UP"));
    }

    @Test
    void testPostValidFeedback() throws SQLException {
        doNothing().when(mockDatabaseService).initDatabase();
        when(mockDatabaseService.saveFeedback(any(FeedbackRequest.class))).thenReturn(1);

        String json = "{\"descricao\": \"ótimo!\", \"nota\": 5}";
        HttpRequestMessage<Optional<String>> request = createMockRequest(HttpMethod.POST, json);

        HttpResponseMessage response = receiver.run(request, mockContext);

        assertEquals(HttpStatus.CREATED, response.getStatus());
        assertTrue(response.getBody().toString().contains("Feedback recorded successfully!"));
    }

    @Test
    void testPostEmptyBody() {
        HttpRequestMessage<Optional<String>> request = createMockRequest(HttpMethod.POST, "");
        HttpResponseMessage response = receiver.run(request, mockContext);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatus());
        assertTrue(response.getBody().toString().contains("Request body cannot be empty"));
    }

    @Test
    void testPostInvalidJson() {
        HttpRequestMessage<Optional<String>> request = createMockRequest(HttpMethod.POST, "invalid-json");
        HttpResponseMessage response = receiver.run(request, mockContext);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatus());
        assertTrue(response.getBody().toString().contains("Invalid JSON format"));
    }

    @Test
    void testPostInvalidRating() {
        String json = "{\"descricao\": \"Nota Acima do esperado\", \"nota\": 11}";
        HttpRequestMessage<Optional<String>> request = createMockRequest(HttpMethod.POST, json);

        HttpResponseMessage response = receiver.run(request, mockContext);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatus());
        assertTrue(response.getBody().toString().contains("rating"));
    }

    @Test
    void testPostNegativeRating() {
        String json = "{\"descricao\": \"Nota negativa\", \"nota\": -1}";
        HttpRequestMessage<Optional<String>> request = createMockRequest(HttpMethod.POST, json);

        HttpResponseMessage response = receiver.run(request, mockContext);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatus());
        assertTrue(response.getBody().toString().contains("rating"));
    }

    @Test
    void testPostMissingDescricao() {
        String json = "{\"nota\": 4}";
        HttpRequestMessage<Optional<String>> request = createMockRequest(HttpMethod.POST, json);

        HttpResponseMessage response = receiver.run(request, mockContext);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatus());
        assertTrue(response.getBody().toString().contains("descricao"));
    }

    @Test
    void testDatabaseErrorReturns500() throws SQLException {
        doThrow(new SQLException("Connection refused")).when(mockDatabaseService).saveFeedback(any(FeedbackRequest.class));

        String json = "{\"descricao\": \"Bom\", \"nota\": 4}";
        HttpRequestMessage<Optional<String>> request = createMockRequest(HttpMethod.POST, json);

        HttpResponseMessage response = receiver.run(request, mockContext);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatus());
        assertTrue(response.getBody().toString().contains("Database error"));
    }
}
