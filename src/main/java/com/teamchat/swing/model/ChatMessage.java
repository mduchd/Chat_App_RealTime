package com.teamchat.swing.model;

public class ChatMessage {
    private String id;
    private String conversationId;
    private String senderId;
    private String senderName;
    private String content;
    private String createdAt;
    private String clientMessageId;
    private boolean sentByMe;

    public ChatMessage() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getClientMessageId() { return clientMessageId; }
    public void setClientMessageId(String clientMessageId) { this.clientMessageId = clientMessageId; }
    public boolean isSentByMe() { return sentByMe; }
    public void setSentByMe(boolean sentByMe) { this.sentByMe = sentByMe; }

    public String getTimeDisplay() {
        if (createdAt == null) return "";
        // Parse ISO datetime and return HH:mm
        try {
            if (createdAt.contains("T")) {
                String timePart = createdAt.substring(createdAt.indexOf('T') + 1);
                if (timePart.length() >= 5) return timePart.substring(0, 5);
            }
        } catch (Exception ignored) {}
        return createdAt;
    }
}
