package com.secondhand.chatservice.service;

import com.secondhand.chatservice.dto.response.ConversationSummaryResponse;
import com.secondhand.chatservice.model.Conversation;

import java.util.List;

public interface ConversationService {
    Conversation createDirectConversation(String initiatorUserId, String participantUserId);

    List<ConversationSummaryResponse> getConversationsByUserId(String userId);
}
