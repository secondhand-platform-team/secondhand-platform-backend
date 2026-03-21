package com.secondhand.chatservice.repository;

import com.secondhand.chatservice.model.Message;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MessageRepository extends MongoRepository<Message, String> {
	List<Message> findByConversationIdOrderByCreatedAtAsc(String conversationId);
}
