package com.justine.serviceImpl;

import com.justine.dtos.request.STKPushRequestDTO;
import com.justine.dtos.response.STKPushResponseDTO;
import com.justine.enums.PaymentMethod;
import com.justine.enums.PaymentStatus;
import com.justine.model.Guest;
import com.justine.model.Payment;
import com.justine.repository.PaymentRepository;
import com.justine.service.MpesaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@Slf4j
public class MpesaServiceImpl implements MpesaService {

    private final PaymentRepository paymentRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${mpesa.consumer-key}")
    private String consumerKey;

    @Value("${mpesa.consumer-secret}")
    private String consumerSecret;

    @Value("${mpesa.shortcode}")
    private String shortcode;

    @Value("${mpesa.passkey}")
    private String passkey;

    @Value("${mpesa.callback-url}")
    private String callbackUrl;

    @Value("${mpesa.environment:sandbox}")
    private String environment;

    public MpesaServiceImpl(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    private String getBaseUrl() {
        return environment.equalsIgnoreCase("production") ?
                "https://api.safaricom.co.ke" :
                "https://sandbox.safaricom.co.ke";
    }

    private String getAccessToken() {
        // Mock token for development
        if (Objects.equals(environment, "sandbox")) {
            return "mock_access_token_" + System.currentTimeMillis();
        }

        String auth = Base64.getEncoder().encodeToString((consumerKey + ":" + consumerSecret).getBytes(StandardCharsets.UTF_8));
        String url = getBaseUrl() + "/oauth/v1/generate?grant_type=client_credentials";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + auth);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
        return (String) response.getBody().get("access_token");
    }

    private String getTimestamp() {
        return new Date().toInstant().toString().replaceAll("[^0-9]", "").substring(0, 14);
    }

    private String generatePassword() {
        String timestamp = getTimestamp();
        return Base64.getEncoder().encodeToString((shortcode + passkey + timestamp).getBytes(StandardCharsets.UTF_8));
    }

    private String formatPhoneNumber(String phone) {
        phone = phone.replaceAll("\\D", "");
        if (phone.startsWith("0")) return "254" + phone.substring(1);
        if (phone.startsWith("+254")) return phone.substring(1);
        if (!phone.startsWith("254")) return "254" + phone;
        return phone;
    }

    private String generateSessionToken() {
        return Long.toHexString(Double.doubleToLongBits(Math.random())) +
                Long.toHexString(Double.doubleToLongBits(Math.random()));
    }

    @Override
    public STKPushResponseDTO initiateSTKPush(STKPushRequestDTO request) {
        String phone = formatPhoneNumber(request.getPhone());
        String password = generatePassword();
        String timestamp = getTimestamp();

        // Mock STK Push for sandbox/development
        if (environment.equalsIgnoreCase("sandbox")) {
            STKPushResponseDTO response = new STKPushResponseDTO();
            response.setMerchantRequestId("mock_merchant_" + System.currentTimeMillis());
            response.setCheckoutRequestId("mock_checkout_" + System.currentTimeMillis());
            response.setResponseCode("0");
            response.setResponseDescription("Success. Request accepted for processing");
            response.setCustomerMessage("Success. Request accepted for processing");

            // Simulate callback after 10 seconds
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    simulatePaymentCallback(response.getCheckoutRequestId(), true);
                }
            }, 10000);

            return response;
        }

        // Real STK Push
        String accessToken = getAccessToken();

        Map<String, Object> payload = new HashMap<>();
        payload.put("BusinessShortCode", shortcode);
        payload.put("Password", password);
        payload.put("Timestamp", timestamp);
        payload.put("TransactionType", "CustomerPayBillOnline");
        payload.put("Amount", Math.round(request.getAmount()));
        payload.put("PartyA", phone);
        payload.put("PartyB", shortcode);
        payload.put("PhoneNumber", phone);
        payload.put("CallBackURL", callbackUrl);
        payload.put("AccountReference", request.getAccountReference());
        payload.put("TransactionDesc", request.getTransactionDesc());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        ResponseEntity<STKPushResponseDTO> response = restTemplate.exchange(
                getBaseUrl() + "/mpesa/stkpush/v1/processrequest",
                HttpMethod.POST,
                entity,
                STKPushResponseDTO.class
        );

        return response.getBody();
    }

    @Override
    public Object querySTKPushStatus(String checkoutRequestId) {
        String accessToken = getAccessToken();
        String timestamp = getTimestamp();
        String password = generatePassword();

        Map<String, Object> payload = new HashMap<>();
        payload.put("BusinessShortCode", shortcode);
        payload.put("Password", password);
        payload.put("Timestamp", timestamp);
        payload.put("CheckoutRequestID", checkoutRequestId);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
        ResponseEntity<Map> response = restTemplate.exchange(
                getBaseUrl() + "/mpesa/stkpushquery/v1/query",
                HttpMethod.POST,
                entity,
                Map.class
        );

        return response.getBody();
    }

    @Override
    public void handleCallback(Object callbackData) {
        try {
            Map<String, Object> data = (Map<String, Object>) callbackData;
            Map<String, Object> body = (Map<String, Object>) data.get("Body");
            Map<String, Object> stkCallback = (Map<String, Object>) body.get("stkCallback");

            String checkoutRequestId = (String) stkCallback.get("CheckoutRequestID");
            int resultCode = (int) stkCallback.get("ResultCode");
            String resultDesc = (String) stkCallback.get("ResultDesc");

            String mpesaReceiptNumber = null;
            Double amount = null;
            String phone = null;

            if (resultCode == 0 && stkCallback.get("CallbackMetadata") != null) {
                Map<String, Object> callbackMetadata = (Map<String, Object>) stkCallback.get("CallbackMetadata");
                List<Map<String, Object>> items = (List<Map<String, Object>>) callbackMetadata.get("Item");

                for (Map<String, Object> item : items) {
                    String name = (String) item.get("Name");
                    Object value = item.get("Value");
                    switch (name) {
                        case "MpesaReceiptNumber":
                            mpesaReceiptNumber = (String) value;
                            break;
                        case "Amount":
                            amount = Double.valueOf(value.toString());
                            break;
                        case "PhoneNumber":
                            phone = value.toString();
                            break;
                    }
                }
            }

            Optional<Payment> paymentOpt = paymentRepository.findByCheckoutRequestId(checkoutRequestId);
            if (paymentOpt.isPresent()) {
                Payment payment = paymentOpt.get();
                payment.setStatus(resultCode == 0 ? PaymentStatus.COMPLETED : PaymentStatus.FAILED);
                payment.setMpesaReceiptNumber(mpesaReceiptNumber);
                paymentRepository.save(payment);

                if (resultCode == 0) {
                    Guest user = payment.getGuest();

                    payment.setPaymentMethod(PaymentMethod.MPESA);
                    payment.setStatus(PaymentStatus.PAID);
                    log.info("Payment successful for userId {}", user.getId());
                }
            } else {
                log.warn("Payment record not found for checkoutRequestId {}", checkoutRequestId);
            }

        } catch (Exception e) {
            log.error("Error handling M-Pesa callback", e);
        }
    }

    private void simulatePaymentCallback(String checkoutRequestId, boolean success) {
        Map<String, Object> mockCallbackData = new HashMap<>();
        Map<String, Object> body = new HashMap<>();
        Map<String, Object> stkCallback = new HashMap<>();

        stkCallback.put("CheckoutRequestID", checkoutRequestId);
        stkCallback.put("ResultCode", success ? 0 : 1);
        stkCallback.put("ResultDesc", success ? "The service request is processed successfully." : "Payment failed");

        if (success) {
            Map<String, Object> callbackMetadata = new HashMap<>();
            List<Map<String, Object>> items = new ArrayList<>();
            items.add(Map.of("Name", "Amount", "Value", 100));
            items.add(Map.of("Name", "MpesaReceiptNumber", "Value", "MOCK" + System.currentTimeMillis()));
            items.add(Map.of("Name", "PhoneNumber", "Value", "254700000000"));
            callbackMetadata.put("Item", items);
            stkCallback.put("CallbackMetadata", callbackMetadata);
        }

        body.put("stkCallback", stkCallback);
        mockCallbackData.put("Body", body);

        handleCallback(mockCallbackData);
        log.info("Mock payment {} for checkoutRequestId {}", success ? "completed" : "failed", checkoutRequestId);
    }
}
