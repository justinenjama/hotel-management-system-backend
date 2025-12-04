package com.justine.wifi.serviceImpl;

import com.justine.wifi.dtos.response.BulkSMSResponseDTO;
import com.justine.wifi.service.SMSService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
public class SMSServiceImpl implements SMSService {

    private final String apiKey;
    private final String username;
    private final String senderId;
    private final RestTemplate restTemplate;

    public SMSServiceImpl(
            @Value("${africastalking.api-key}") String apiKey,
            @Value("${africastalking.username}") String username,
            @Value("${africastalking.sender-id}") String senderId,
            RestTemplateBuilder builder
    ) {
        this.apiKey = apiKey;
        this.username = username;
        this.senderId = senderId;
        this.restTemplate = builder.build();

        log.info("AT username: {}", username);
        log.info("AT apiKey: {}...", apiKey != null ? apiKey.substring(0, 6) : "null");
    }

    @Override
    public boolean sendSMS(String phone, String message) {
        try {
            phone = formatPhoneNumber(phone);

            Map<String, Object> body = new HashMap<>();
            body.put("username", username);
            body.put("to", Collections.singletonList(phone));
            body.put("message", message);
            body.put("from", senderId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apiKey", apiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            restTemplate.postForEntity("https://api.africastalking.com/version1/messaging", request, String.class);

            log.info("SMS sent successfully to {}", phone);
            return true;
        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", phone, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean sendPaymentConfirmation(String phone, double amount, String receiptNumber) {
        String message = String.format("Payment confirmed! KES %.2f received. Receipt: %s. Your internet access is now active. - COLLOSPOT", amount, receiptNumber);
        return sendSMS(phone, message);
    }

    @Override
    public boolean sendSessionExpiry(String phone, String planName) {
        String message = String.format("Your %s internet session has expired. Visit our portal to purchase a new plan and continue browsing. - COLLOSPOT", planName);
        return sendSMS(phone, message);
    }

    @Override
    public boolean sendWelcomeMessage(String phone, String firstName) {
        String name = firstName != null ? " " + firstName : "";
        String message = String.format("Welcome%s to COLLOSPOT! Your account has been created successfully. Connect to our WiFi and enjoy high-speed internet. - COLLOSPOT", name);
        return sendSMS(phone, message);
    }

    @Override
    public boolean sendOTP(String phone, String otp) {
        String message = String.format("Your COLLOSPOT verification code is: %s. This code expires in 10 minutes. Do not share this code with anyone.", otp);
        return sendSMS(phone, message);
    }

    @Override
    public boolean sendLowBalanceAlert(String phone, String remainingTime) {
        String message = String.format("Your internet session expires in %s. Top up now to avoid disconnection. Visit our portal to purchase more data. - COLLOSPOT", remainingTime);
        return sendSMS(phone, message);
    }

    @Override
    public boolean sendPasswordReset(String phone, String resetCode) {
        String message = String.format("Your COLLOSPOT password reset code is: %s. Use this code to reset your password. Code expires in 15 minutes.", resetCode);
        return sendSMS(phone, message);
    }

    @Override
    public BulkSMSResponseDTO sendBulkSMS(String[] phones, String message) {
        int success = 0;
        int failed = 0;

        for (String phone : phones) {
            boolean sent = sendSMS(phone, message);
            if (sent) success++; else failed++;
            try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
        }

        BulkSMSResponseDTO response = new BulkSMSResponseDTO();
        response.setSuccess(success);
        response.setFailed(failed);
        return response;
    }

    private String formatPhoneNumber(String phone) {
        phone = phone.replaceAll("\\D", "");
        if (phone.startsWith("0")) phone = "+254" + phone.substring(1);
        else if (phone.startsWith("254")) phone = "+" + phone;
        else if (!phone.startsWith("+254")) phone = "+254" + phone;
        return phone;
    }
}
