package com.smartcommerce.consumer;

import com.smartcommerce.config.RabbitMQConfig;
import com.smartcommerce.event.OrderCreatedEvent;
import com.smartcommerce.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final EmailService emailService;

    @RabbitListener(queues = RabbitMQConfig.ORDER_CREATED_QUEUE)
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Received order.created event for order: {}", event.getOrderNumber());
        emailService.sendOrderConfirmation(event);
    }
}
