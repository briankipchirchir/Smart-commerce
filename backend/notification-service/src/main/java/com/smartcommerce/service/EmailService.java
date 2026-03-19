package com.smartcommerce.service;

import com.smartcommerce.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    @Value("${resend.api.key}")
    private String resendApiKey;

    @Value("${resend.from.email}")
    private String fromEmail;

    private final RestTemplate restTemplate;

    public void sendOrderConfirmation(OrderCreatedEvent event) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(resendApiKey);

            String html = buildOrderConfirmationHtml(event);

            Map<String, Object> body = Map.of(
                "from", fromEmail,
                "to", new String[]{event.getCustomerEmail()},
                "subject", "Order Confirmed — " + event.getOrderNumber(),
                "html", html
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(
                "https://api.resend.com/emails", request, String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Order confirmation email sent to: {}", event.getCustomerEmail());
            } else {
                log.error("Failed to send email, status: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", event.getCustomerEmail(), e.getMessage());
        }
    }

    private String buildOrderConfirmationHtml(OrderCreatedEvent event) {
        String itemsHtml = "";
        if (event.getItems() != null && !event.getItems().isEmpty()) {
            itemsHtml = event.getItems().stream().map(item -> """
                <tr>
                  <td style="padding:10px;border-bottom:1px solid #eee;">%s</td>
                  <td style="padding:10px;border-bottom:1px solid #eee;text-align:center;">%d</td>
                  <td style="padding:10px;border-bottom:1px solid #eee;text-align:right;">$%s</td>
                </tr>
                """.formatted(
                    item.getProductName(),
                    item.getQuantity(),
                    item.getTotalPrice().toPlainString()
                )).collect(Collectors.joining());
        }

        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"/>
            <style>
              body { font-family: Arial, sans-serif; background: #f4f4f4; margin: 0; padding: 0; }
              .container { max-width: 600px; margin: 40px auto; background: white; border-radius: 8px; overflow: hidden; }
              .header { background: #1a1a2e; color: white; padding: 30px; text-align: center; }
              .body { padding: 30px; }
              .order-box { background: #f8f9fa; border-radius: 8px; padding: 20px; margin: 20px 0; }
              .order-number { font-size: 22px; font-weight: bold; color: #1a1a2e; margin-bottom: 10px; }
              .total { font-size: 20px; color: #28a745; font-weight: bold; margin-top: 15px; }
              table { width: 100%%; border-collapse: collapse; margin-top: 10px; }
              th { background: #1a1a2e; color: white; padding: 10px; text-align: left; }
              .footer { background: #f4f4f4; padding: 20px; text-align: center; color: #888; font-size: 12px; }
            </style>
            </head>
            <body>
            <div class="container">
              <div class="header"><h1>⚡ ShopSmart</h1><p>Order Confirmed!</p></div>
              <div class="body">
                <p>Hi <strong>%s</strong>,</p>
                <p>Your order has been placed successfully.</p>
                <div class="order-box">
                  <div class="order-number">%s</div>
                  <p>Delivering to: <strong>%s, %s</strong></p>
                  <table>
                    <thead>
                      <tr>
                        <th>Product</th>
                        <th style="text-align:center;">Qty</th>
                        <th style="text-align:right;">Price</th>
                      </tr>
                    </thead>
                    <tbody>%s</tbody>
                  </table>
                  <div class="total">Total: $%s</div>
                </div>
                <p>Thank you for shopping with ShopSmart!</p>
              </div>
              <div class="footer"><p>© 2026 ShopSmart. All rights reserved.</p></div>
            </div>
            </body>
            </html>
            """.formatted(
                event.getCustomerFirstName(),
                event.getOrderNumber(),
                event.getShippingCity(),
                event.getShippingCountry(),
                itemsHtml,
                event.getTotal().toPlainString()
            );
    }
}
