package com.secondhand.chatservice.service.impl;

import com.secondhand.chatservice.model.Conversation;
import com.secondhand.chatservice.repository.ConversationRepository;
import com.secondhand.chatservice.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private final ConversationRepository conversationRepository;

    @Override
    public Conversation createDirectConversation(String initiatorUserId, String participantUserId) {
        if (initiatorUserId == null || initiatorUserId.isBlank()) {
            throw new IllegalArgumentException("initiatorUserId is required");
        }
        if (participantUserId == null || participantUserId.isBlank()) {
            throw new IllegalArgumentException("participantUserId is required");
        }
        if (initiatorUserId.equals(participantUserId)) {
            throw new IllegalArgumentException("Cannot create conversation with self");
        }

        String conversationKey = buildConversationKey(initiatorUserId, participantUserId);

        return conversationRepository.findByConversationKey(conversationKey)
                .orElseGet(() -> createNewConversation(initiatorUserId, participantUserId, conversationKey));
    }

    private Conversation createNewConversation(String initiatorUserId, String participantUserId, String conversationKey) {
        LocalDateTime now = LocalDateTime.now();
        Conversation conversation = Conversation.builder()
                .participants(List.of(initiatorUserId, participantUserId))
                .conversationKey(conversationKey)
                .createdAt(now)
                .updatedAt(now)
                .build();

        try {
            return conversationRepository.save(conversation);
        } catch (DuplicateKeyException exception) {
            return conversationRepository.findByConversationKey(conversationKey)
                    .orElseThrow(() -> exception);
        }
    }

    private String buildConversationKey(String firstUserId, String secondUserId) {
        if (firstUserId.compareTo(secondUserId) < 0) {
            return firstUserId + ":" + secondUserId;
        }
        return secondUserId + ":" + firstUserId;
    }
}
