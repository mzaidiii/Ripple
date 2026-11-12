package org.personal.ripple.controller;

import lombok.RequiredArgsConstructor;
import org.personal.ripple.entity.RepoStatus;
import org.personal.ripple.entity.TrackedRepo;
import org.personal.ripple.repository.TrackedRepoRepository;
import org.personal.ripple.services.IndexRunner;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/repos")
@RequiredArgsConstructor
public class RepoController {

    private final IndexRunner indexRunner;
    private final TrackedRepoRepository trackedRepoRepository;

    @PostMapping("/{owner}/{repo}/index")
    public ResponseEntity<String> index(@PathVariable String owner, @PathVariable String repo){

        String name = owner +"/" + repo ;

        if(!isValid(owner , repo)){
            return ResponseEntity.badRequest().body("Invalid repository name");
        }

        if (trackedRepoRepository.findByFullName(name).isEmpty()) {
            TrackedRepo created = new TrackedRepo();
            created.setFullName(name);
            trackedRepoRepository.save(created);
        }

        int ans = trackedRepoRepository.markIndexing(name);
        if (ans == 0 ){
            return ResponseEntity.status(409).body("Repo is already indexing");
        } else {
            indexRunner.run(name,"HEAD");

        }
        return ResponseEntity.accepted().body("Indexing started");
    }

    @GetMapping("/{owner}/{repo}")
    public ResponseEntity<?> status(@PathVariable String owner, @PathVariable String repo) {
        if (!isValid(owner, repo)) {
            return ResponseEntity.badRequest().body("Invalid repository name");
        }

        return trackedRepoRepository.findByFullName(owner + "/" + repo)
                .<ResponseEntity<?>>map(r -> ResponseEntity.ok(new RepoStatusResponse(
                        r.getFullName(),
                        r.getStatus(),
                        r.getLastIndexedSha(),
                        r.getIndexedAt(),
                        r.getLastError())))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private boolean isValid(String owner, String repo) {
        if (owner.equals(".") || owner.equals("..") || repo.equals(".") || repo.equals("..")) {
            return false;
        }
        return (owner + "/" + repo).matches("^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$");
    }

    private record RepoStatusResponse(String fullName, RepoStatus status, String lastIndexedSha,
                                      Instant indexedAt, String lastError) {}
}
