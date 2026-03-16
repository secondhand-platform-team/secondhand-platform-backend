package com.secondhand.chatservice.repository;

import com.secondhand.chatservice.model.Message;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MessageRepository extends MongoRepository<Message, String> {
}
