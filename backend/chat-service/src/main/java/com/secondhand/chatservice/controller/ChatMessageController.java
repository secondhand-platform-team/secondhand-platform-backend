package com.secondhand.chatservice.controller;

import com.secondhand.chatservice.dto.request.ToggleReactionRequest;
import com.secondhand.chatservice.dto.websocket.ChatMessageResponse;
import com.secondhand.chatservice.security.JwtAuthenticatedUser;
import com.secondhand.chatservice.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping("/{messageId}/reactions")
    public ChatMessageResponse toggleReaction(
            @PathVariable String messageId,
            @RequestBody ToggleReactionRequest request,
            Authentication authentication
    ) {
        JwtAuthenticatedUser currentUser = (JwtAuthenticatedUser) authentication.getPrincipal();
        ChatMessageResponse response = chatMessageService.toggleReaction(
                messageId,
                currentUser.userId(),
                request.getEmoji()
        );

        messagingTemplate.convertAndSend(
                "/topic/conversations/" + response.getConversationId(),
                response
        );

        return response;
    }
}