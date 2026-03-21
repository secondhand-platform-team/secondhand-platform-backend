package com.secondhand.authservice.service.impl;

import com.secondhand.authservice.exception.BadRequestException;
import com.secondhand.authservice.grpc.ConversationGrpcClient;
import com.secondhand.authservice.model.User;
import com.secondhand.authservice.repository.UserRepository;
import com.secondhand.authservice.service.ChatConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatConversationServiceImpl implements ChatConversationService {

    private final UserRepository userRepository;
    private final ConversationGrpcClient conversationGrpcClient;

    @Override
    public String createConversation(String initiatorEmail, String participantUserId) {
        User initiator = userRepository.findByEmail(initiatorEmail)
                .orElseThrow(() -> new BadRequestException("User not found"));

        return conversationGrpcClient.createConversation(initiator.getUserId(), participantUserId);
    }
}
