package com.secondhand.chatservice.controller;

import com.secondhand.chatservice.dto.response.ConversationSummaryResponse;
import com.secondhand.chatservice.dto.response.MessageHistoryResponse;
import com.secondhand.chatservice.security.JwtAuthenticatedUser;
import com.secondhand.chatservice.service.ChatMessageService;
import com.secondhand.chatservice.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ChatConversationController {

    private final ConversationService conversationService;
    private final ChatMessageService chatMessageService;

    @GetMapping("/me")
    public List<ConversationSummaryResponse> getMyConversations(Authentication authentication) {
        JwtAuthenticatedUser currentUser = (JwtAuthenticatedUser) authentication.getPrincipal();
        return conversationService.getConversationsByUserId(currentUser.userId());
    }

    @GetMapping("/{conversationId}/messages")
    public List<MessageHistoryResponse> getConversationMessages(
            @PathVariable String conversationId,
            Authentication authentication
    ) {
        JwtAuthenticatedUser currentUser = (JwtAuthenticatedUser) authentication.getPrincipal();
        return chatMessageService.getMessagesByConversationId(conversationId, currentUser.userId());
    }
}
