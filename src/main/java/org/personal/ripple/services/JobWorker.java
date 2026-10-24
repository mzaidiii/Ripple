package org.personal.ripple.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class JobWorker {

    private final JobClaimService jobClaimService;

    @Scheduled(fixedDelay = 5000)
    void poll(){
        jobClaimService.claim().ifPresent(job ->{
                    log.info("Received the job with id {} and name {}" , job.getId(),job.getRepoFullName());
                    jobClaimService.markDone(job.getId());
                }
                );
    }
}
