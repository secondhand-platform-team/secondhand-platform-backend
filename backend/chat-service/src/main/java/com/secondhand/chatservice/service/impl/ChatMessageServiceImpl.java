package com.secondhand.chatservice.service.impl;

import com.secondhand.chatservice.dto.common.MessageReactionDto;
import com.secondhand.chatservice.dto.common.ReplyMessageDto;
import com.secondhand.chatservice.dto.response.MessageHistoryResponse;
import com.secondhand.chatservice.dto.websocket.ChatMessageRequest;
import com.secondhand.chatservice.dto.websocket.ChatMessageResponse;
import com.secondhand.chatservice.model.Conversation;
import com.secondhand.chatservice.model.Message;
import com.secondhand.chatservice.model.MessageReaction;
import com.secondhand.chatservice.model.enums.MessageStatus;
import com.secondhand.chatservice.model.enums.MessageType;
import com.secondhand.chatservice.repository.ConversationRepository;
import com.secondhand.chatservice.repository.MessageRepository;
import com.secondhand.chatservice.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
            .replyToMessageId(request.getReplyToMessageId())
            .reactions(new ArrayList<>())
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

        ReplyMessageDto replyTo = resolveReplyMessage(saved, null);
        return toChatMessageResponse(saved, replyTo);
    }

    @Override
    public List<MessageHistoryResponse> getMessagesByConversationId(String conversationId, String userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        if (conversation.getParticipants() == null || !conversation.getParticipants().contains(userId)) {
            throw new IllegalArgumentException("You do not have permission to access this conversation");
        }

        List<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        Map<String, Message> messageMapById = messages.stream()
                .collect(Collectors.toMap(Message::getId, Function.identity(), (left, right) -> left));

        return messages.stream()
                .map(message -> MessageHistoryResponse.builder()
                        .messageId(message.getId())
                        .conversationId(message.getConversationId())
                        .senderId(message.getSenderId())
                        .receiverId(message.getReceiverId())
                        .content(message.getContent())
                        .type(message.getType())
                        .status(message.getStatus())
                        .createdAt(message.getCreatedAt())
                        .replyTo(resolveReplyMessage(message, messageMapById))
                        .reactions(mapReactions(message.getReactions()))
                        .build())
                .toList();
    }

    @Override
    public ChatMessageResponse toggleReaction(String messageId, String userId, String emoji) {
        if (messageId == null || messageId.isBlank()) {
            throw new IllegalArgumentException("messageId is required");
        }

        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }

        if (emoji == null || emoji.isBlank()) {
            throw new IllegalArgumentException("emoji is required");
        }

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));

        Conversation conversation = conversationRepository.findById(message.getConversationId())
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        if (conversation.getParticipants() == null || !conversation.getParticipants().contains(userId)) {
            throw new IllegalArgumentException("You do not have permission to react to this message");
        }

        List<MessageReaction> reactions = message.getReactions() == null
                ? new ArrayList<>()
                : new ArrayList<>(message.getReactions());

        int existingIndex = -1;
        for (int index = 0; index < reactions.size(); index++) {
            MessageReaction reaction = reactions.get(index);
            if (userId.equals(reaction.getUserId()) && emoji.equals(reaction.getEmoji())) {
                existingIndex = index;
                break;
            }
        }

        if (existingIndex >= 0) {
            reactions.remove(existingIndex);
        } else {
            reactions.add(MessageReaction.builder()
                    .emoji(emoji)
                    .userId(userId)
                    .userName(null)
                    .build());
        }

        message.setReactions(reactions);
        Message saved = messageRepository.save(message);

        return toChatMessageResponse(saved, resolveReplyMessage(saved, null));
    }

    private ChatMessageResponse toChatMessageResponse(Message message, ReplyMessageDto replyTo) {
        return ChatMessageResponse.builder()
                .messageId(message.getId())
                .conversationId(message.getConversationId())
                .senderId(message.getSenderId())
                .receiverId(message.getReceiverId())
                .content(message.getContent())
                .type(message.getType())
                .status(message.getStatus())
                .createdAt(message.getCreatedAt())
                .replyTo(replyTo)
                .reactions(mapReactions(message.getReactions()))
                .build();
    }

    private ReplyMessageDto resolveReplyMessage(Message message, Map<String, Message> messageMapById) {
        if (message.getReplyToMessageId() == null || message.getReplyToMessageId().isBlank()) {
            return null;
        }

        Message replyMessage = messageMapById != null
                ? messageMapById.get(message.getReplyToMessageId())
                : null;

        if (replyMessage == null) {
            replyMessage = messageRepository.findById(message.getReplyToMessageId()).orElse(null);
        }

        if (replyMessage == null) {
            return null;
        }

        return ReplyMessageDto.builder()
                .messageId(replyMessage.getId())
                .senderId(replyMessage.getSenderId())
                .senderName(null)
                .content(replyMessage.getContent())
                .messageType(replyMessage.getType())
                .isDeleted(false)
                .build();
    }

    private List<MessageReactionDto> mapReactions(List<MessageReaction> reactions) {
        if (reactions == null || reactions.isEmpty()) {
            return List.of();
        }

        return reactions.stream()
                .map(reaction -> MessageReactionDto.builder()
                        .emoji(reaction.getEmoji())
                        .userId(reaction.getUserId())
                        .userName(reaction.getUserName())
                        .build())
                .toList();
    }
}
