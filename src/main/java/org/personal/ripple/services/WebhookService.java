package org.personal.ripple.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.personal.ripple.entity.AnalysisJob;
import org.personal.ripple.entity.DeliveryStatus;
import org.personal.ripple.entity.JobStatus;
import org.personal.ripple.entity.WebhookDelivery;
import org.personal.ripple.repository.AnalysisJobRepository;
import org.personal.ripple.repository.WebhookDeliveryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
@Slf4j
@RequiredArgsConstructor
public class WebhookService {

    private final WebhookDeliveryRepository webRepo;
    private final ObjectMapper objectMapper;
    private final AnalysisJobRepository analysisJobRepository;

    @Transactional
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
            throw new IllegalArgumentException("Invalid JSON payload", e);
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
        makeJob(delivery);
    }

    private void makeJob(WebhookDelivery delivery){
        AnalysisJob analysisJob = new AnalysisJob();
        analysisJob.setDeliveryId(delivery.getDeliveryId());
        analysisJob.setRepoFullName(delivery.getRepoFullName());
        analysisJob.setBeforeSha(delivery.getBeforeSha());
        analysisJob.setAfterSha(delivery.getAfterSha());

        analysisJobRepository.save(analysisJob);
        log.info("Job Created Successfully with delivery id {} and Repo name {}" , delivery.getDeliveryId() , delivery.getRepoFullName());
    }
}