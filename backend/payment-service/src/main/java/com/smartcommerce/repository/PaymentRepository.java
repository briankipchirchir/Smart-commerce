package com.smartcommerce.repository;

import com.smartcommerce.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrderNumber(String orderNumber);
    Optional<Payment> findByStripePaymentIntentId(String paymentIntentId);
    Optional<Payment> findByMpesaCheckoutRequestId(String checkoutRequestId);
}
