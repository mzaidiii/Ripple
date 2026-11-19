package org.personal.ripple.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.personal.ripple.dto.ChatRequest;
import org.personal.ripple.dto.ChatResponse;
import org.personal.ripple.entity.RepoStatus;
import org.personal.ripple.entity.TrackedRepo;
import org.personal.ripple.llm.EmbeddingClient;
import org.personal.ripple.llm.LlmClient;
import org.personal.ripple.repository.ChunkHit;
import org.personal.ripple.repository.CodeChunkRepository;
import org.personal.ripple.repository.TrackedRepoRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private EmbeddingClient embeddingClient;

    @Mock
    private CodeChunkRepository codeChunkRepository;

    @Mock
    private TrackedRepoRepository trackedRepoRepository;

    @Mock
    private LlmClient llmClient;

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(embeddingClient, codeChunkRepository, trackedRepoRepository, llmClient);
    }

    @Test
    void ask_whenRepoNotReady_returnsNotReadyMessage() {
        when(trackedRepoRepository.findByFullName("owner/repo")).thenReturn(Optional.empty());
        when(llmClient.modelName()).thenReturn("gemini-3.5-flash-lite");

        ChatResponse response = chatService.ask("owner/repo", new ChatRequest("how does auth work?", 5, 0.5));

        assertFalse(response.contextFound());
        assertTrue(response.answer().contains("not ready for chat"));
        assertTrue(response.citations().isEmpty());
        verifyNoInteractions(embeddingClient);
    }

    @Test
    void ask_whenNoChunksPassThreshold_returnsNoContextMessage() {
        TrackedRepo repo = new TrackedRepo();
        repo.setFullName("owner/repo");
        repo.setStatus(RepoStatus.READY);

        when(trackedRepoRepository.findByFullName("owner/repo")).thenReturn(Optional.of(repo));
        when(embeddingClient.embedQuery("how does auth work?")).thenReturn(new float[]{0.1f, 0.2f});
        when(llmClient.modelName()).thenReturn("gemini-3.5-flash-lite");

        ChunkHit lowScoreHit = mock(ChunkHit.class);
        when(lowScoreHit.getScore()).thenReturn(0.35); // below default threshold 0.55
        when(codeChunkRepository.findNearestHits(eq("owner/repo"), anyString(), eq(5)))
                .thenReturn(List.of(lowScoreHit));

        ChatResponse response = chatService.ask("owner/repo", new ChatRequest("how does auth work?", null, null));

        assertFalse(response.contextFound());
        assertTrue(response.answer().contains("could not find any code"));
        assertTrue(response.citations().isEmpty());
        verify(llmClient, never()).complete(anyString());
    }

    @Test
    void ask_whenRelevantChunksFound_callsLlmAndReturnsCitations() {
        TrackedRepo repo = new TrackedRepo();
        repo.setFullName("owner/repo");
        repo.setStatus(RepoStatus.READY);

        when(trackedRepoRepository.findByFullName("owner/repo")).thenReturn(Optional.of(repo));
        when(embeddingClient.embedQuery("how does auth work?")).thenReturn(new float[]{0.1f, 0.2f});
        when(llmClient.modelName()).thenReturn("gemini-3.5-flash-lite");

        ChunkHit hit = mock(ChunkHit.class);
        when(hit.getFilePath()).thenReturn("src/Auth.java");
        when(hit.getStartLine()).thenReturn(10);
        when(hit.getEndLine()).thenReturn(30);
        when(hit.getContent()).thenReturn("public boolean isValid() { return true; }");
        when(hit.getScore()).thenReturn(0.85);

        when(codeChunkRepository.findNearestHits(eq("owner/repo"), anyString(), eq(3)))
                .thenReturn(List.of(hit));

        when(llmClient.complete(anyString())).thenReturn("Auth works using isValid method.");

        ChatResponse response = chatService.ask("owner/repo", new ChatRequest("how does auth work?", 3, 0.70));

        assertTrue(response.contextFound());
        assertEquals("Auth works using isValid method.", response.answer());
        assertEquals(1, response.citations().size());
        assertEquals("src/Auth.java", response.citations().get(0).filePath());
        assertEquals(10, response.citations().get(0).startLine());
        assertEquals(30, response.citations().get(0).endLine());
        assertEquals(0.85, response.citations().get(0).score());
        verify(llmClient).complete(argThat(prompt ->
                prompt.contains("src/Auth.java") && prompt.contains("public boolean isValid()")
        ));
    }
}
