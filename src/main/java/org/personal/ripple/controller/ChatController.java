package org.personal.ripple.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.personal.ripple.dto.ChatRequest;
import org.personal.ripple.dto.ChatResponse;
import org.personal.ripple.services.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/repos")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/{owner}/{repo}/chat")
    public ResponseEntity<?> ask(
            @PathVariable String owner,
            @PathVariable String repo,
            @Valid @RequestBody ChatRequest request) {

        if (!isValidRepoName(owner, repo)) {
            return ResponseEntity.badRequest().body("Invalid repository name");
        }

        String repoFullName = owner + "/" + repo;
        ChatResponse response = chatService.ask(repoFullName, request);
        return ResponseEntity.ok(response);
    }

    private boolean isValidRepoName(String owner, String repo) {
        if (owner.equals(".") || owner.equals("..") || repo.equals(".") || repo.equals("..")) {
            return false;
        }
        return (owner + "/" + repo).matches("^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$");
    }
}
