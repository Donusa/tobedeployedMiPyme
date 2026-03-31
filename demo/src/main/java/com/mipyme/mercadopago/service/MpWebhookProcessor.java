package com.mipyme.mercadopago.service;

import com.mipyme.mercadopago.model.MpWebhookEvent;
import com.mipyme.mercadopago.repository.MpWebhookEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class MpWebhookProcessor {

    private static final Logger logger = LoggerFactory.getLogger(MpWebhookProcessor.class);

    private final MpWebhookEventRepository eventRepository;
    private final MpPaymentProcessor paymentProcessor;
    private final MpSubscriptionProcessor subscriptionProcessor;
    private final MpClaimProcessor claimProcessor;
    private final MpChargebackProcessor chargebackProcessor;

    public MpWebhookProcessor(
            MpWebhookEventRepository eventRepository,
            MpPaymentProcessor paymentProcessor,
            MpSubscriptionProcessor subscriptionProcessor,
            MpClaimProcessor claimProcessor,
            MpChargebackProcessor chargebackProcessor) {
        this.eventRepository = eventRepository;
        this.paymentProcessor = paymentProcessor;
        this.subscriptionProcessor = subscriptionProcessor;
        this.claimProcessor = claimProcessor;
        this.chargebackProcessor = chargebackProcessor;
    }

    @Async
    public void processEventAsync(Long eventId) {
        logger.info("Async processing started for Webhook Event ID: {}", eventId);

        eventRepository.findById(eventId).ifPresent(event -> {
            if (event.getStatus() != MpWebhookEvent.WebhookStatus.RECEIVED &&
                    event.getStatus() != MpWebhookEvent.WebhookStatus.FAILED) {
                logger.warn("Event {} is already in status {}, skipping.", eventId, event.getStatus());
                return;
            }

            event.setStatus(MpWebhookEvent.WebhookStatus.PROCESSING);
            event.setAttempts(event.getAttempts() + 1);
            eventRepository.save(event);

            try {
                processEvent(event);

                event.setStatus(MpWebhookEvent.WebhookStatus.PROCESSED);
                event.setProcessedAt(LocalDateTime.now());
                event.setLastError(null);

                logger.info("Successfully processed Webhook Event ID: {}", eventId);
            } catch (Exception e) {
                logger.error("Error processing Webhook Event ID: " + eventId, e);
                event.setStatus(MpWebhookEvent.WebhookStatus.FAILED);
                event.setLastError(e.getMessage());
            } finally {
                eventRepository.save(event);
            }
        });
    }

    private void processEvent(MpWebhookEvent event) throws Exception {
        String type = event.getType();
        String action = event.getAction();
        String dataId = event.getDataId();

        logger.info("Dispatching event Type: {}, Action: {}, DataId: {}", type, action, dataId);

        switch (type) {
            case "payment":
                logger.info("-> Routing to Payment Processor for payment {}", dataId);
                paymentProcessor.processPayment(dataId);
                break;
            case "subscription_preapproval":
            case "subscription_preapproval_plan":
                logger.info("-> Routing to Subscription Processor for subscription {}", dataId);
                subscriptionProcessor.processSubscription(dataId, type);
                break;
            case "claim":
                logger.info("-> Routing to Claim Processor for claim {}", dataId);
                claimProcessor.processClaim(dataId);
                break;
            case "chargeback":
                logger.info("-> Routing to Chargeback Processor for chargeback {}", dataId);
                chargebackProcessor.processChargeback(dataId);
                break;
            default:
                logger.warn("Unhandled webhook type: {}. Marking as processed to ignore.", type);
                break;
        }
    }
}
