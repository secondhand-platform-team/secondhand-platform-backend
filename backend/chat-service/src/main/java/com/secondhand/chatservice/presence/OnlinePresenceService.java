package com.secondhand.chatservice.presence;

import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OnlinePresenceService {

    private final ConcurrentHashMap<String, Set<String>> userSessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> sessionToUser = new ConcurrentHashMap<>();

    public Optional<PresenceChange> registerSession(String userId, String sessionId) {
        if (isBlank(userId) || isBlank(sessionId)) {
            return Optional.empty();
        }

        sessionToUser.put(sessionId, userId);
        Set<String> sessions = userSessions.computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet());
        sessions.add(sessionId);

        if (sessions.size() == 1) {
            return Optional.of(new PresenceChange(userId, true));
        }

        return Optional.empty();
    }

    public Optional<PresenceChange> unregisterSession(String sessionId) {
        if (isBlank(sessionId)) {
            return Optional.empty();
        }

        String userId = sessionToUser.remove(sessionId);

        if (isBlank(userId)) {
            return Optional.empty();
        }

        Set<String> sessions = userSessions.get(userId);

        if (sessions == null) {
            return Optional.empty();
        }

        sessions.remove(sessionId);

        if (!sessions.isEmpty()) {
            return Optional.empty();
        }

        userSessions.remove(userId, sessions);
        return Optional.of(new PresenceChange(userId, false));
    }

    public boolean isUserOnline(String userId) {
        if (isBlank(userId)) {
            return false;
        }

        Set<String> sessions = userSessions.get(userId);
        return sessions != null && !sessions.isEmpty();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record PresenceChange(String userId, boolean isOnline) {
    }
}
