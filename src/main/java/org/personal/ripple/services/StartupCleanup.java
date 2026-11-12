package org.personal.ripple.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.personal.ripple.repository.TrackedRepoRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class StartupCleanup {

    private final TrackedRepoRepository trackedRepoRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void failInterruptedIndexing() {
        int n = trackedRepoRepository.failInterrupted();
        if (n > 0) {
            log.warn("Marked {} interrupted indexing run(s) as FAILED", n);
        }
    }
}