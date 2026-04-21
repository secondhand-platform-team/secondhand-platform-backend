package com.secondhand.authservice.client;

import com.secondhand.authservice.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@Component
@Slf4j
public class ConversationRestClient {

    private final RestClient restClient;

    public ConversationRestClient(@Value("${chat.service.url}") String chatServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(chatServiceUrl)
                .build();
    }

    public String createConversation(String initiatorUserId, String participantUserId) {
        try {
            ConversationResponse response = restClient.post()
                    .uri("/api/internal/conversations")
                    .body(Map.of(
                            "initiatorUserId", initiatorUserId,
                            "participantUserId", participantUserId
                    ))
                    .retrieve()
                    .body(ConversationResponse.class);

            if (response == null || response.conversationId() == null) {
                throw new BadRequestException("Cannot create conversation: empty response");
            }

            log.info("Conversation created: id={}", response.conversationId());
            return response.conversationId();
        } catch (RestClientException e) {
            log.error("Failed to create conversation. initiatorUserId={}, participantUserId={}",
                    initiatorUserId, participantUserId, e);
            throw new BadRequestException("Cannot create conversation");
        }
    }

    private record ConversationResponse(String conversationId) {}
}
