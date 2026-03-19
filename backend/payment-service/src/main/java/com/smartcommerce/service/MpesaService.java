package com.smartcommerce.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MpesaService {

    @Value("${mpesa.consumer.key}")
    private String consumerKey;

    @Value("${mpesa.consumer.secret}")
    private String consumerSecret;

    @Value("${mpesa.shortcode}")
    private String shortcode;

    @Value("${mpesa.passkey}")
    private String passkey;

    @Value("${mpesa.callback.url}")
    private String callbackUrl;

    private final RestTemplate restTemplate;

    private static final String SANDBOX_BASE = "https://sandbox.safaricom.co.ke";

    public String getAccessToken() {
        String credentials = Base64.getEncoder()
                .encodeToString((consumerKey + ":" + consumerSecret).getBytes());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + credentials);

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(
                SANDBOX_BASE + "/oauth/v1/generate?grant_type=client_credentials",
                HttpMethod.GET, entity, Map.class
        );

        return (String) response.getBody().get("access_token");
    }

    public String stkPush(String phone, BigDecimal amount, String orderNumber) {
        String accessToken = getAccessToken();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String password = Base64.getEncoder().encodeToString(
                (shortcode + passkey + timestamp).getBytes()
        );

        // Format phone: 0712345678 → 254712345678
        String formattedPhone = phone.startsWith("0")
                ? "254" + phone.substring(1)
                : phone.replace("+", "");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        Map<String, Object> body = new HashMap<>();
        body.put("BusinessShortCode", shortcode);
        body.put("Password", password);
        body.put("Timestamp", timestamp);
        body.put("TransactionType", "CustomerPayBillOnline");
        body.put("Amount", amount.intValue());
        body.put("PartyA", formattedPhone);
        body.put("PartyB", shortcode);
        body.put("PhoneNumber", formattedPhone);
        body.put("CallBackURL", callbackUrl);
        body.put("AccountReference", orderNumber);
        body.put("TransactionDesc", "Payment for order " + orderNumber);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(
                SANDBOX_BASE + "/mpesa/stkpush/v1/processrequest",
                request, Map.class
        );

        log.info("STK Push response: {}", response.getBody());
        return (String) response.getBody().get("CheckoutRequestID");
    }
}
