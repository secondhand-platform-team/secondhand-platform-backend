package com.secondhand.chatservice.repository;

import com.secondhand.chatservice.model.Conversation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ConversationRepository extends MongoRepository<Conversation, String> {
    Optional<Conversation> findByConversationKey(String conversationKey);
}
