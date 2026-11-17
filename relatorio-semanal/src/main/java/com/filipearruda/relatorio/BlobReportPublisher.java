package com.filipearruda.relatorio;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.options.BlobParallelUploadOptions;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class BlobReportPublisher implements ReportPublisher {
    private static final ZoneId BRASILIA = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(BRASILIA);

    @Override
    public void publish(String json) {
        String connection = System.getenv("AzureWebJobsStorage");
        if (connection == null || connection.isBlank()) {
            throw new IllegalStateException("App setting AzureWebJobsStorage não está configurada.");
        }

        BlobContainerClient container = new BlobContainerClientBuilder()
                .connectionString(connection)
                .containerName("relatorios")
                .buildClient();
        container.createIfNotExists();

        upload(container, "ultimo-relatorio.json", json);
        upload(container, "relatorio-" + STAMP.format(Instant.now()) + ".json", json);
    }

    private static void upload(BlobContainerClient container, String name, String json) {
        BlobParallelUploadOptions options = new BlobParallelUploadOptions(BinaryData.fromString(json))
                .setHeaders(new BlobHttpHeaders().setContentType("application/json"));
        container.getBlobClient(name).uploadWithResponse(options, null, null);
    }
}
