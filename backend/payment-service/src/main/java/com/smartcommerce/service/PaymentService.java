package com.smartcommerce.service;

import com.smartcommerce.config.RabbitMQConfig;
import com.smartcommerce.dto.InitiatePaymentRequest;
import com.smartcommerce.dto.PaymentResponse;
import com.smartcommerce.entity.Payment;
import com.smartcommerce.entity.PaymentMethod;
import com.smartcommerce.entity.PaymentStatus;
import com.smartcommerce.event.PaymentCompletedEvent;
import com.smartcommerce.exception.ResourceNotFoundException;
import com.smartcommerce.repository.PaymentRepository;
import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RabbitTemplate rabbitTemplate;
    private final MpesaService mpesaService;

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    @Transactional
    public PaymentResponse initiatePayment(String userId, String email,
                                           InitiatePaymentRequest request) {
        PaymentMethod method = PaymentMethod.valueOf(request.getMethod().toUpperCase());

        Payment payment = Payment.builder()
                .orderNumber(request.getOrderNumber())
                .userId(userId)
                .customerEmail(email)
                .amount(request.getAmount())
                .method(method)
                .status(PaymentStatus.PENDING)
                .build();

        return switch (method) {
            case STRIPE -> initiateStripe(payment, request);
            case MPESA -> initiateMpesa(payment, request);
            case COD -> initiateCod(payment);
        };
    }

    private PaymentResponse initiateStripe(Payment payment, InitiatePaymentRequest request) {
        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(request.getAmount().multiply(new java.math.BigDecimal("100")).longValue())
                    .setCurrency("usd")
                    .putMetadata("orderNumber", request.getOrderNumber())
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);
            payment.setStripePaymentIntentId(intent.getId());
            payment.setStripeClientSecret(intent.getClientSecret());
            Payment saved = paymentRepository.save(payment);

            log.info("Stripe payment initiated for order: {}", request.getOrderNumber());
            return toResponse(saved);
        } catch (Exception e) {
            log.error("Stripe initiation failed: {}", e.getMessage());
            throw new RuntimeException("Failed to initiate Stripe payment: " + e.getMessage());
        }
    }

    private PaymentResponse initiateMpesa(Payment payment, InitiatePaymentRequest request) {
        try {
            String phone = request.getMpesaPhone();
            String checkoutRequestId = mpesaService.stkPush(
                    phone,
                    request.getAmount(),
                    request.getOrderNumber()
            );
            payment.setMpesaPhone(phone);
            payment.setMpesaCheckoutRequestId(checkoutRequestId);
            Payment saved = paymentRepository.save(payment);

            log.info("M-Pesa STK Push initiated for order: {}", request.getOrderNumber());
            return toResponse(saved);
        } catch (Exception e) {
            log.error("M-Pesa initiation failed: {}", e.getMessage());
            throw new RuntimeException("Failed to initiate M-Pesa payment: " + e.getMessage());
        }
    }

    private PaymentResponse initiateCod(Payment payment) {
        payment.setStatus(PaymentStatus.COMPLETED);
        Payment saved = paymentRepository.save(payment);
        publishPaymentCompleted(saved);
        log.info("COD payment confirmed for order: {}", payment.getOrderNumber());
        return toResponse(saved);
    }

    @Transactional
    public void handleStripeWebhook(String orderNumber) {
        Payment payment = paymentRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        payment.setStatus(PaymentStatus.COMPLETED);
        paymentRepository.save(payment);
        publishPaymentCompleted(payment);
        log.info("Stripe payment completed for order: {}", orderNumber);
    }

    @Transactional
    public void handleMpesaCallback(String checkoutRequestId, boolean success, String receiptNumber) {
        Payment payment = paymentRepository.findByMpesaCheckoutRequestId(checkoutRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        if (success) {
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setMpesaReceiptNumber(receiptNumber);
            publishPaymentCompleted(payment);
            log.info("M-Pesa payment completed for order: {}", payment.getOrderNumber());
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            log.warn("M-Pesa payment failed for order: {}", payment.getOrderNumber());
        }
        paymentRepository.save(payment);
    }

    private void publishPaymentCompleted(Payment payment) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PAYMENT_EXCHANGE,
                RabbitMQConfig.PAYMENT_COMPLETED_ROUTING_KEY,
                PaymentCompletedEvent.builder()
                        .orderNumber(payment.getOrderNumber())
                        .customerEmail(payment.getCustomerEmail())
                        .amount(payment.getAmount())
                        .method(payment.getMethod().name())
                        .build()
        );
    }

    private PaymentResponse toResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderNumber(payment.getOrderNumber())
                .amount(payment.getAmount())
                .method(payment.getMethod())
                .status(payment.getStatus())
                .stripeClientSecret(payment.getStripeClientSecret())
                .mpesaCheckoutRequestId(payment.getMpesaCheckoutRequestId())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
