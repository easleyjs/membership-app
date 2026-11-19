package com.easleyjs.membership.service;

import com.easleyjs.membership.exception.EmailDeliveryException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class ResendClient {
    private static final String EMAILS_URL = "https://api.resend.com/emails";
    private static final String DEFAULT_FROM = "Outland <onboarding@resend.dev>";

    private final Environment environment;
    private final RestClient client;

    @Autowired
    public ResendClient(Environment environment) {
        this(environment, createClient());
    }

    ResendClient(Environment environment, RestClient client) {
        this.environment = environment;
        this.client = client;
    }

    public String sendEmail(String recipient, String subject, String html, String qrContentId, byte[] qrPng) {
        String apiKey = environment.getProperty("RESEND_API_KEY");
        if (!StringUtils.hasText(apiKey)) {
            throw new EmailDeliveryException("Set RESEND_API_KEY in dev.env or the application environment.");
        }

        var attachment = Map.of(
                "filename", "member-qr.png",
                "content_id", qrContentId,
                "content", Base64.getEncoder().encodeToString(qrPng));
        var payload = Map.of(
                "from", environment.getProperty("RESEND_FROM", DEFAULT_FROM),
                "to", List.of(recipient),
                "subject", subject,
                "html", html,
                "attachments", List.of(attachment));

        SendResponse response;
        try {
            response = client.post().uri(EMAILS_URL)
                    .headers(headers -> headers.setBearerAuth(apiKey))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(SendResponse.class);
        } catch (RestClientResponseException failure) {
            throw new EmailDeliveryException(describeRejection(failure));
        } catch (RestClientException failure) {
            throw new EmailDeliveryException("Could not complete the request to Resend.", failure);
        }

        if (response == null || !StringUtils.hasText(response.id())) {
            throw new EmailDeliveryException("Resend did not return an email ID.");
        }
        return response.id();
    }

    private static String describeRejection(RestClientResponseException failure) {
        String summary = "Resend rejected the email (HTTP " + failure.getStatusCode().value() + ").";
        if (failure.getStatusCode() != HttpStatus.FORBIDDEN) {
            return summary;
        }

        ErrorResponse error;
        try {
            error = failure.getResponseBodyAs(ErrorResponse.class);
        } catch (RestClientException unrecognizedBody) {
            return summary;
        }
        if (error == null || error.name() == null) {
            return summary;
        }

        return switch (error.name()) {
            case "invalid_permission", "restricted_api_key" ->
                    summary + " Check that RESEND_API_KEY is active and can send from the configured domain.";
            case "suspended_api_key" -> summary + " The Resend API key is suspended.";
            case "validation_error" -> summary + senderValidationHint(error.message());
            default -> summary;
        };
    }

    private static String senderValidationHint(String message) {
        String detail = message == null ? "" : message.toLowerCase(Locale.ROOT);
        if (detail.startsWith("you can only send testing emails to your own email address")) {
            return " Use a test member whose email matches your Resend account, or set RESEND_FROM to an address on a verified domain.";
        }
        if (detail.contains("domain is not verified")) {
            return " Set RESEND_FROM to an address on a domain verified for sending in Resend.";
        }
        return "";
    }

    private static RestClient createClient() {
        var httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        return RestClient.builder().requestFactory(requestFactory).build();
    }

    private record SendResponse(String id) {}

    private record ErrorResponse(String name, String message) {}
}
