package com.justine.service;

import com.justine.dtos.response.BulkSMSResponseDTO;
public interface SMSService {

    boolean sendSMS(String phone, String message);

    BulkSMSResponseDTO sendBulkSMS(String[] phones, String message);
}

