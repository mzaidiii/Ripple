package org.personal.ripple.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.personal.ripple.entity.RepoStatus;
import org.personal.ripple.entity.TrackedRepo;
import org.personal.ripple.github.GitHubClient;
import org.personal.ripple.repository.TrackedRepoRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@Slf4j
@RequiredArgsConstructor
public class IndexRunner {

    private final IndexService indexService;
    private final GitHubClient gitHubClient;
    private final TrackedRepoRepository trackedRepoRepository;

    @Async
    public void run(String repoFullName, String ref) {
        try {
            String sha = gitHubClient.getCommitSha(repoFullName, ref);
            indexService.indexRepo(repoFullName, sha);

            TrackedRepo repo = trackedRepoRepository.findByFullName(repoFullName).orElseThrow();
            repo.setStatus(RepoStatus.READY);
            repo.setLastIndexedSha(sha);
            repo.setIndexedAt(Instant.now());
            repo.setLastError(null);
            trackedRepoRepository.save(repo);

            log.info("Repository {} is ready at {}", repoFullName, sha);
        } catch (Exception e) {
            log.error("Indexing failed for {}", repoFullName, e);

            trackedRepoRepository.findByFullName(repoFullName).ifPresent(repo -> {
                repo.setStatus(RepoStatus.FAILED);
                repo.setLastError(e.toString());
                trackedRepoRepository.save(repo);
            });
        }
    }
}