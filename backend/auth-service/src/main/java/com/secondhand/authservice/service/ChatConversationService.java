package com.secondhand.authservice.service;

public interface ChatConversationService {
    String createConversation(String initiatorEmail, String participantUserId);
}
