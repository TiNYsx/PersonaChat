package com.tinysx.personachat;

import java.util.UUID;

/**
 * Represents a single chat message sent by a player.
 */
public class ChatMessage {

    private final String senderName;
    private final UUID senderUUID;
    private final String rawMessage;
    private final long timestamp;

    public ChatMessage(String senderName, UUID senderUUID, String rawMessage) {
        this.senderName = senderName;
        this.senderUUID = senderUUID;
        this.rawMessage = rawMessage;
        this.timestamp = System.currentTimeMillis();
    }

    public String getSenderName() {
        return senderName;
    }

    public UUID getSenderUUID() {
        return senderUUID;
    }

    public String getRawMessage() {
        return rawMessage;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isExpired(long lifetimeMs) {
        return (System.currentTimeMillis() - timestamp) > lifetimeMs;
    }
}
