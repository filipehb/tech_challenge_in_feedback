package com.filipearruda.mail;

import com.azure.communication.email.EmailClient;
import com.azure.communication.email.EmailClientBuilder;
import com.azure.communication.email.models.EmailAddress;
import com.azure.communication.email.models.EmailMessage;
import com.filipearruda.dto.AvaliacaoCriticaMessage;

public final class AzureEmailNotifier implements EmailNotifier {
    @Override
    public void send(AvaliacaoCriticaMessage message) {
        String connectionString = required("AZURE_COMMUNICATION_CONNECTION_STRING");
        String sender = required("ACS_SENDER_ADDRESS");
        String admin = required("ADMIN_EMAIL");

        EmailClient client = new EmailClientBuilder()
                .connectionString(connectionString)
                .buildClient();

        EmailMessage email = new EmailMessage()
                .setSenderAddress(sender)
                .setToRecipients(new EmailAddress(admin))
                .setSubject(EmailComposer.subject(message))
                .setBodyPlainText(EmailComposer.body(message));

        client.beginSend(email).waitForCompletion();
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("App setting " + name + " não está configurada.");
        }
        return value;
    }
}
