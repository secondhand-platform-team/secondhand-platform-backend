package com.secondhand.chatservice.controller;

import com.secondhand.chatservice.model.Conversation;
import com.secondhand.chatservice.service.ConversationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/internal/conversations")
@RequiredArgsConstructor
public class InternalConversationController {

    private final ConversationService conversationService;

    @PostMapping
    public ResponseEntity<Map<String, String>> createConversation(@Valid @RequestBody CreateConversationRequest request) {
        Conversation conversation = conversationService.createDirectConversation(
                request.initiatorUserId(), request.participantUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("conversationId", conversation.getId()));
    }

    public record CreateConversationRequest(
            @NotBlank String initiatorUserId,
            @NotBlank String participantUserId
    ) {}
}
