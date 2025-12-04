package com.justine.wifi.service;

import com.justine.wifi.dtos.response.BulkSMSResponseDTO;

public interface SMSService {

    boolean sendSMS(String phone, String message);

    boolean sendPaymentConfirmation(String phone, double amount, String receiptNumber);

    boolean sendSessionExpiry(String phone, String planName);

    boolean sendWelcomeMessage(String phone, String firstName);

    boolean sendOTP(String phone, String otp);

    boolean sendLowBalanceAlert(String phone, String remainingTime);

    boolean sendPasswordReset(String phone, String resetCode);

    BulkSMSResponseDTO sendBulkSMS(String[] phones, String message);
}

