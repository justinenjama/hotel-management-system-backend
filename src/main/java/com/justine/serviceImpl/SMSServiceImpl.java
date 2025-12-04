package com.justine.serviceImpl;

import com.justine.dtos.response.BulkSMSResponseDTO;
import com.justine.service.SMSService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;

@Slf4j
@Service
public class SMSServiceImpl implements SMSService {

    private final String bearerToken;
    private final String senderId;
    private final String callbackUrl;
    private final RestTemplate restTemplate;
    private final String smsApiUrl;

    public SMSServiceImpl(
            @Value("${sms.provider.bearer-token}") String bearerToken,
            @Value("${sms.provider.sender-id}") String senderId,
            @Value("${sms.provider.callback-url}") String callbackUrl,
            @Value("${sms.provider.api-url}") String smsApiUrl,
            RestTemplateBuilder builder
    ) {
        this.bearerToken = bearerToken;
        this.senderId = senderId;
        this.callbackUrl = callbackUrl;
        this.smsApiUrl = smsApiUrl;
        this.restTemplate = builder.build();
    }

    @Override
    public boolean sendSMS(String phone, String message) {
        try {
            phone = formatPhoneNumber(phone);

            // Build JSON payload
            String payload = "{"
                    + "\"message\": \"" + escapeJson(message) + "\","
                    + "\"to\": \"" + phone + "\","
                    + "\"bypass_optout\": true,"
                    + "\"sender_id\": \"" + senderId + "\","
                    + "\"callback_url\": \"" + callbackUrl + "\""
                    + "}";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(bearerToken);

            HttpEntity<String> request = new HttpEntity<>(payload, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    smsApiUrl,
                    request,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ SMS queued successfully to {}: {}", phone, response.getBody());
                return true;
            } else {
                log.error("❌ SMS failed [{}] to {}: {}", response.getStatusCode(), phone, response.getBody());
                return false;
            }

        } catch (Exception e) {
            log.error("❌ Failed to send SMS to {}: {}", phone, e.getMessage());
            return false;
        }
    }

    @Override
    public BulkSMSResponseDTO sendBulkSMS(String[] phones, String message) {
        int success = 0;
        int failed = 0;

        for (String phone : phones) {
            boolean sent = sendSMS(phone, message);
            if (sent) {
                success++;
            } else {
                failed++;
            }

            // Optional pause to avoid hitting provider rate limits
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        }

        BulkSMSResponseDTO response = new BulkSMSResponseDTO();
        response.setSuccess(success);
        response.setFailed(failed);
        return response;
    }

    private String formatPhoneNumber(String phone) {
        phone = phone.replaceAll("\\D", "");
        if (phone.startsWith("0")) {
            phone = "+254" + phone.substring(1);
        } else if (phone.startsWith("254")) {
            phone = "+" + phone;
        } else if (!phone.startsWith("+254")) {
            phone = "+254" + phone;
        }
        return phone;
    }

    private String escapeJson(String text) {
        // Escape quotes and backslashes for JSON
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
