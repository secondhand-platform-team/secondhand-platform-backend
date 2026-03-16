package com.secondhand.chatservice.websocket;

import com.secondhand.chatservice.dto.websocket.ChatMessageRequest;
import com.secondhand.chatservice.dto.websocket.ChatMessageResponse;
import com.secondhand.chatservice.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessageRequest request) {
        ChatMessageResponse response = chatMessageService.sendMessage(request);
        messagingTemplate.convertAndSend(
                "/topic/conversations/" + response.getConversationId(),
                response);
    }
}
