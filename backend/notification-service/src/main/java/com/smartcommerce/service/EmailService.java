package com.smartcommerce.service;

import com.smartcommerce.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.internet.MimeMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    public void sendOrderConfirmation(OrderCreatedEvent event) {
        try {
            Context context = new Context();
            context.setVariable("firstName", event.getCustomerFirstName());
            context.setVariable("orderNumber", event.getOrderNumber());
            context.setVariable("total", event.getTotal());
            context.setVariable("shippingCity", event.getShippingCity());
            context.setVariable("shippingCountry", event.getShippingCountry());

            String html = templateEngine.process("order-confirmation", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(event.getCustomerEmail());
            helper.setSubject("Order Confirmed — " + event.getOrderNumber());
            helper.setText(html, true);

            mailSender.send(message);
            log.info("Order confirmation email sent to: {}", event.getCustomerEmail());
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", event.getCustomerEmail(), e.getMessage());
        }
    }
}
