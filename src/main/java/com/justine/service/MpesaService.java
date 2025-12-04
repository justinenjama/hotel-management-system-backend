package com.justine.service;

import com.justine.dtos.request.STKPushRequestDTO;
import com.justine.dtos.response.STKPushResponseDTO;

public interface MpesaService {

    STKPushResponseDTO initiateSTKPush(STKPushRequestDTO request);

    Object querySTKPushStatus(String checkoutRequestId);

    void handleCallback(Object callbackData);
}

