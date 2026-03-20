package com.smartcommerce.consumer;

import com.smartcommerce.config.RabbitMQConfig;
import com.smartcommerce.entity.OrderStatus;
import com.smartcommerce.event.PaymentCompletedEvent;
import com.smartcommerce.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final OrderRepository orderRepository;

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_COMPLETED_QUEUE)
    @Transactional
    public void handlePaymentCompleted(@Payload PaymentCompletedEvent event) {
        try {
            log.info("Received payment.completed event for order: {}", event.getOrderNumber());
            orderRepository.findByOrderNumber(event.getOrderNumber()).ifPresent(order -> {
                order.setStatus(OrderStatus.PROCESSING);
                orderRepository.save(order);
                log.info("Order {} status updated to PROCESSING", event.getOrderNumber());
            });
        } catch (Exception e) {
            log.error("Error processing payment event: {}", e.getMessage(), e);
        }
    }
}
