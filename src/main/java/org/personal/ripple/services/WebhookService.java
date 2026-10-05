package org.personal.ripple.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.personal.ripple.entity.DeliveryStatus;
import org.personal.ripple.entity.WebhookDelivery;
import org.personal.ripple.repository.WebhookDeliveryRepository;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
@Slf4j
@RequiredArgsConstructor
public class WebhookService {

    private final WebhookDeliveryRepository webRepo;
    private final ObjectMapper objectMapper;

    public void handlePush(String deliveryId, String payload) {

        if (webRepo.existsByDeliveryId(deliveryId)) {
            log.info("Duplicate delivery {}", deliveryId);
            return;
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(payload);
        } catch (JacksonException e) {
            log.error("Invalid JSON payload for delivery {}", deliveryId);
            throw new IllegalArgumentException("Invalid JSON payload");
        }

        String ref = root.path("ref").asString();
        String before = root.path("before").asString();
        String after = root.path("after").asString();
        String repoFullName = root.path("repository").path("full_name").asString();

        log.info("Push received: repo={}, ref={}, before={}, after={}",
                repoFullName, ref, before, after);


        WebhookDelivery delivery = new WebhookDelivery();
        delivery.setDeliveryId(deliveryId);
        delivery.setEventType("push");
        delivery.setRepoFullName(repoFullName);
        delivery.setRef(ref);
        delivery.setBeforeSha(before);
        delivery.setAfterSha(after);
        delivery.setPayload(payload);
        delivery.setStatus(DeliveryStatus.RECEIVED);

        webRepo.save(delivery);
    }
}