package com.secondhand.authservice.grpc;

import com.secondhand.authservice.exception.BadRequestException;
import com.secondhand.authservice.grpc.conversation.ConversationServiceGrpc;
import com.secondhand.authservice.grpc.conversation.CreateConversationRequest;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ConversationGrpcClient {

    @GrpcClient("chat-service")
    private ConversationServiceGrpc.ConversationServiceBlockingStub conversationServiceBlockingStub;

    public String createConversation(String initiatorUserId, String participantUserId) {
        try {
            CreateConversationRequest request = CreateConversationRequest.newBuilder()
                    .setInitiatorUserId(initiatorUserId)
                    .setParticipantUserId(participantUserId)
                    .build();

            return conversationServiceBlockingStub.createConversation(request).getConversationId();
        } catch (StatusRuntimeException exception) {
            log.error("Create conversation gRPC call failed. initiatorUserId={}, participantUserId={}",
                    initiatorUserId,
                    participantUserId,
                    exception);
            throw new BadRequestException("Cannot create conversation", exception);
        }
    }
}
