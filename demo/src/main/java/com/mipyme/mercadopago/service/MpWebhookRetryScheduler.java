package com.mipyme.mercadopago.service;

import com.mipyme.mercadopago.model.MpWebhookEvent;
import com.mipyme.mercadopago.repository.MpWebhookEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MpWebhookRetryScheduler {

    private static final Logger logger = LoggerFactory.getLogger(MpWebhookRetryScheduler.class);
    private static final int MAX_ATTEMPTS = 5;

    private final MpWebhookEventRepository eventRepository;
    private final MpWebhookProcessor webhookProcessor;

    public MpWebhookRetryScheduler(MpWebhookEventRepository eventRepository, MpWebhookProcessor webhookProcessor) {
        this.eventRepository = eventRepository;
        this.webhookProcessor = webhookProcessor;
    }

    @Scheduled(fixedRate = 600000)
    public void retryFailedWebhooks() {
        List<MpWebhookEvent> failed = eventRepository.findByStatusAndAttemptsLessThan(
                MpWebhookEvent.WebhookStatus.FAILED, MAX_ATTEMPTS);

        if (failed.isEmpty()) {
            return;
        }

        logger.info("Retrying {} failed webhook events", failed.size());

        for (MpWebhookEvent event : failed) {
            try {
                webhookProcessor.processEventAsync(event.getId());
            } catch (Exception e) {
                logger.error("Error retrying webhook event {}: {}", event.getId(), e.getMessage());
            }
        }
    }
}
