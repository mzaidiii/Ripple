package org.personal.ripple.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.personal.ripple.dto.ChatRequest;
import org.personal.ripple.dto.ChatResponse;
import org.personal.ripple.dto.Citation;
import org.personal.ripple.services.ChatService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private ChatService chatService;

    private ChatController chatController;

    @BeforeEach
    void setUp() {
        chatController = new ChatController(chatService);
    }

    @Test
    void ask_withValidRepoAndRequest_returnsOkWithResponse() {
        ChatResponse expectedResponse = new ChatResponse(
                "Auth is implemented via HMAC.",
                List.of(new Citation("src/Auth.java", 1, 20, 0.91, "public class Auth")),
                "gemini-3.5-flash-lite",
                true
        );

        ChatRequest request = new ChatRequest("Explain auth", 5, 0.6);
        when(chatService.ask(eq("octocat/Hello-World"), eq(request))).thenReturn(expectedResponse);

        ResponseEntity<?> response = chatController.ask("octocat", "Hello-World", request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedResponse, response.getBody());
        verify(chatService).ask("octocat/Hello-World", request);
    }

    @Test
    void ask_withInvalidRepoName_returnsBadRequest() {
        ChatRequest request = new ChatRequest("Explain auth", 5, 0.6);

        ResponseEntity<?> dotResponse = chatController.ask(".", "Hello-World", request);
        assertEquals(HttpStatus.BAD_REQUEST, dotResponse.getStatusCode());

        ResponseEntity<?> dotDotResponse = chatController.ask("octocat", "..", request);
        assertEquals(HttpStatus.BAD_REQUEST, dotDotResponse.getStatusCode());

        ResponseEntity<?> invalidCharsResponse = chatController.ask("octo cat", "Hello", request);
        assertEquals(HttpStatus.BAD_REQUEST, invalidCharsResponse.getStatusCode());

        verifyNoInteractions(chatService);
    }
}
