package com.secondhand.chatservice.grpc;

import com.secondhand.chatservice.grpc.conversation.ConversationServiceGrpc;
import com.secondhand.chatservice.grpc.conversation.CreateConversationRequest;
import com.secondhand.chatservice.grpc.conversation.CreateConversationResponse;
import com.secondhand.chatservice.model.Conversation;
import com.secondhand.chatservice.service.ConversationService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Map;

@GrpcService
@RequiredArgsConstructor
public class ConversationGrpcService extends ConversationServiceGrpc.ConversationServiceImplBase {

    private final ConversationService conversationService;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void createConversation(CreateConversationRequest request,
                                   StreamObserver<CreateConversationResponse> responseObserver) {
        try {
            Conversation conversation = conversationService.createDirectConversation(
                    request.getInitiatorUserId(),
                    request.getParticipantUserId());

            // Notify the participant (user B) so they see the new conversation in real-time
            messagingTemplate.convertAndSend(
                    "/topic/users/" + request.getParticipantUserId() + "/conversations",
                    Map.of("event", "new_conversation", "conversationId", conversation.getId()));

            CreateConversationResponse response = CreateConversationResponse.newBuilder()
                    .setConversationId(conversation.getId())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (IllegalArgumentException exception) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(exception.getMessage())
                    .asRuntimeException());
        } catch (Exception exception) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Unable to create conversation")
                    .withCause(exception)
                    .asRuntimeException());
        }
    }
}
