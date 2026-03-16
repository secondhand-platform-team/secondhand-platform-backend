package com.secondhand.chatservice.service;

import com.secondhand.chatservice.model.Conversation;

public interface ConversationService {
    Conversation createDirectConversation(String initiatorUserId, String participantUserId);
}
