package com.secondhand.chatservice.service.impl;

import com.secondhand.chatservice.dto.websocket.ChatMessageRequest;
import com.secondhand.chatservice.dto.websocket.ChatMessageResponse;
import com.secondhand.chatservice.model.Conversation;
import com.secondhand.chatservice.model.Message;
import com.secondhand.chatservice.model.enums.MessageStatus;
import com.secondhand.chatservice.model.enums.MessageType;
import com.secondhand.chatservice.repository.ConversationRepository;
import com.secondhand.chatservice.repository.MessageRepository;
import com.secondhand.chatservice.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ChatMessageServiceImpl implements ChatMessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;

    @Override
    public ChatMessageResponse sendMessage(ChatMessageRequest request) {
        if (request.getConversationId() == null || request.getConversationId().isBlank()) {
            throw new IllegalArgumentException("conversationId is required");
        }
        if (request.getSenderId() == null || request.getSenderId().isBlank()) {
            throw new IllegalArgumentException("senderId is required");
        }
        if (request.getReceiverId() == null || request.getReceiverId().isBlank()) {
            throw new IllegalArgumentException("receiverId is required");
        }
        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new IllegalArgumentException("content is required");
        }

        MessageType messageType = request.getType() == null ? MessageType.TEXT : request.getType();

        Message message = Message.builder()
                .conversationId(request.getConversationId())
                .senderId(request.getSenderId())
                .receiverId(request.getReceiverId())
                .content(request.getContent())
                .type(messageType)
                .status(MessageStatus.SENT)
                .createdAt(LocalDateTime.now())
                .build();

        Message saved = messageRepository.save(message);

        conversationRepository.findById(saved.getConversationId()).ifPresent(conversation -> {
            conversation.setLastMessageId(saved.getId());
            conversation.setLastMessageAt(saved.getCreatedAt());
            conversation.setUpdatedAt(LocalDateTime.now());
            conversationRepository.save(conversation);
        });

        return ChatMessageResponse.builder()
                .messageId(saved.getId())
                .conversationId(saved.getConversationId())
                .senderId(saved.getSenderId())
                .receiverId(saved.getReceiverId())
                .content(saved.getContent())
                .type(saved.getType())
                .status(saved.getStatus())
                .createdAt(saved.getCreatedAt())
                .build();
    }
}
