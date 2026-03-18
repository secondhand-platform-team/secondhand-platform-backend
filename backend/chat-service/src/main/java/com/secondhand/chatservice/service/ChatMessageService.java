package com.secondhand.chatservice.service;

import com.secondhand.chatservice.dto.response.MessageHistoryResponse;
import com.secondhand.chatservice.dto.websocket.ChatMessageRequest;
import com.secondhand.chatservice.dto.websocket.ChatMessageResponse;

import java.util.List;

public interface ChatMessageService {
    ChatMessageResponse sendMessage(ChatMessageRequest request);

    List<MessageHistoryResponse> getMessagesByConversationId(String conversationId, String userId);

    ChatMessageResponse toggleReaction(String messageId, String userId, String emoji);
}
