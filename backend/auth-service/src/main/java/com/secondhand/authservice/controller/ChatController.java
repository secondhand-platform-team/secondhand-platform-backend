package com.secondhand.authservice.controller;

import com.secondhand.authservice.dto.request.ConversationCreateRequest;
import com.secondhand.authservice.dto.response.ConversationCreateResponse;
import com.secondhand.authservice.service.ChatConversationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatConversationService chatConversationService;

    @PostMapping("/conversations")
    public ResponseEntity<ConversationCreateResponse> createConversation(
            @Valid @RequestBody ConversationCreateRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String initiatorEmail = authentication.getName();

        String conversationId = chatConversationService.createConversation(initiatorEmail, request.getUserId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ConversationCreateResponse(conversationId));
    }
}
