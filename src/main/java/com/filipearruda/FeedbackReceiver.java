package com.filipearruda;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.filipearruda.dto.FeedbackRequest;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;

import java.sql.SQLException;
import java.util.Optional;

public class FeedbackReceiver {

    private final ObjectMapper objectMapper;
    private final DatabaseService databaseService;

    public FeedbackReceiver() {
        this(new ObjectMapper(), new DatabaseService());
    }

    public FeedbackReceiver(ObjectMapper objectMapper, DatabaseService databaseService) {
        this.objectMapper = objectMapper;
        this.databaseService = databaseService;
    }

    @FunctionName("ReceberAvaliacao")
    public HttpResponseMessage run(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.GET, HttpMethod.POST},
                    authLevel = AuthorizationLevel.ANONYMOUS
            )
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        context.getLogger().info("Processing request for ReceberAvaliacao.");

        if (request.getHttpMethod() == HttpMethod.GET) {
            return request.createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body("{\"status\": \"UP\", \"service\": \"ReceberAvaliacao\"}")
                    .build();
        }

        String body = request.getBody().orElse("");
        if (body.trim().isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .header("Content-Type", "application/json")
                    .body("{\"error\": \"Request body cannot be empty.\"}")
                    .build();
        }

        FeedbackRequest feedback;
        try {
            feedback = objectMapper.readValue(body, FeedbackRequest.class);
        } catch (Exception e) {
            context.getLogger().warning("Invalid JSON payload: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .header("Content-Type", "application/json")
                    .body("{\"error\": \"Invalid JSON format: " + e.getMessage() + "\"}")
                    .build();
        }

        // Validate payload
        if (feedback.getDescription() == null || feedback.getDescription().trim().isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .header("Content-Type", "application/json")
                    .body("{\"error\": \"'descricao' is required.\"}")
                    .build();
        }

        if (feedback.getRate() < 0 || feedback.getRate() > 10) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .header("Content-Type", "application/json")
                    .body("{\"error\": \"'rating' is required and must be between 0 and 10.\"}")
                    .build();
        }

        try {
            databaseService.initDatabase();
            databaseService.saveFeedback(feedback);
            return request.createResponseBuilder(HttpStatus.CREATED)
                    .header("Content-Type", "application/json")
                    .body("{\"message\": \"Feedback recorded successfully!\"}")
                    .build();
        } catch (SQLException e) {
            context.getLogger().severe("Database error while saving feedback: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("Content-Type", "application/json")
                    .body("{\"error\": \"Database error: " + e.getMessage() + "\"}")
                    .build();
        }
    }
}
