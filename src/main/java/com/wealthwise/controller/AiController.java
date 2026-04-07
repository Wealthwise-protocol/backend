package com.wealthwise.controller;

import com.wealthwise.dto.request.ChatRequest;
import com.wealthwise.dto.response.ChatResponse;
import com.wealthwise.dto.response.MessageDto;
import com.wealthwise.service.AiService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody ChatRequest request
    ) {
        UUID userUuid = getAuthenticatedUserId(userId);
        String response = aiService.chat(userUuid, request.getMessage());
        return ResponseEntity.ok(ChatResponse.builder().response(response).build());
    }

    @GetMapping("/history")
    public ResponseEntity<Map<String, List<MessageDto>>> getHistory(
            @AuthenticationPrincipal String userId
    ) {
        UUID userUuid = getAuthenticatedUserId(userId);
        List<MessageDto> history = aiService.getHistory(userUuid);
        return ResponseEntity.ok(Map.of("messages", history));
    }

    @DeleteMapping("/history")
    public ResponseEntity<Map<String, Boolean>> clearHistory(
            @AuthenticationPrincipal String userId
    ) {
        UUID userUuid = getAuthenticatedUserId(userId);
        aiService.clearHistory(userUuid);
        return ResponseEntity.ok(Map.of("success", true));
    }

    private UUID getAuthenticatedUserId(String userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        try {
            return UUID.fromString(userId);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
    }
}
