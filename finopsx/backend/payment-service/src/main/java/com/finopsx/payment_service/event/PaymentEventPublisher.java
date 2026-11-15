package com.finopsx.payment_service.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends payment events to Kafka only after the database transaction commits,
 * so consumers never see an event for a payment that was rolled back.
 */
@Component
public class PaymentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventPublisher.class);

    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;
    private final String topic;

    public PaymentEventPublisher(KafkaTemplate<String, PaymentEvent> kafkaTemplate,
                                 @Value("${finopsx.kafka.payments-topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(PaymentEvent event) {
        // Keyed by payment id so all events of one payment land on the same partition, in order.
        kafkaTemplate.send(topic, event.paymentId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish {} for payment {}", event.type(), event.paymentId(), ex);
                    } else {
                        log.info("Published {} for payment {} to {}", event.type(), event.paymentId(), topic);
                    }
                });
    }
}
