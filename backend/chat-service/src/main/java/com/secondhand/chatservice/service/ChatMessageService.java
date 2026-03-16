package com.secondhand.chatservice.service;

import com.secondhand.chatservice.dto.websocket.ChatMessageRequest;
import com.secondhand.chatservice.dto.websocket.ChatMessageResponse;

public interface ChatMessageService {
    ChatMessageResponse sendMessage(ChatMessageRequest request);
}
