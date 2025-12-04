package com.justine.wifi.service;

import com.justine.wifi.dtos.request.STKPushRequestDTO;
import com.justine.wifi.dtos.response.STKPushResponseDTO;

public interface MpesaService {

    STKPushResponseDTO initiateSTKPush(STKPushRequestDTO request);

    Object querySTKPushStatus(String checkoutRequestId);

    void handleCallback(Object callbackData);
}

