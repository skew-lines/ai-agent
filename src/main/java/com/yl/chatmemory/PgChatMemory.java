package com.yl.chatmemory;

import com.yl.persistence.ChatSessionStore;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class PgChatMemory implements ChatMemory {

    private static final int MAX_MESSAGES = 20;

    private final JdbcTemplate jdbcTemplate;
    private final ChatSessionStore chatSessionStore;

    public PgChatMemory(JdbcTemplate jdbcTemplate, ChatSessionStore chatSessionStore) {
        this.jdbcTemplate = jdbcTemplate;
        this.chatSessionStore = chatSessionStore;
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        if (conversationId == null || conversationId.isBlank() || messages == null || messages.isEmpty()) {
            return;
        }
        Long sessionPk = chatSessionStore.getOrCreateSession(conversationId, "love_app", buildTitle(conversationId));
        for (Message message : messages) {
            chatSessionStore.saveMessage(sessionPk, message);
        }
        chatSessionStore.touchSession(sessionPk);
    }

    @Override
    public List<Message> get(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return Collections.emptyList();
        }
        Long sessionPk = chatSessionStore.findSessionId(conversationId);
        if (sessionPk == null) {
            return Collections.emptyList();
        }
        List<Message> messages = jdbcTemplate.query("""
                        select role, content, message_type
                        from (
                            select role, content, message_type, created_at, id
                            from chat_message
                            where session_id = ?
                            order by created_at desc, id desc
                            limit ?
                        ) t
                        order by created_at asc, id asc
                        """,
                (rs, rowNum) -> toMessage(
                        rs.getString("role"),
                        rs.getString("content"),
                        rs.getString("message_type")
                ),
                sessionPk,
                MAX_MESSAGES
        );
        return new ArrayList<>(messages);
    }

    @Override
    public void clear(String conversationId) {
        Long sessionPk = chatSessionStore.findSessionId(conversationId);
        if (sessionPk == null) {
            return;
        }
        chatSessionStore.deleteSession(sessionPk);
    }

    private Message toMessage(String role, String content, String messageType) {
        String normalized = role == null ? messageType : role;
        if ("system".equalsIgnoreCase(normalized)) {
            return new SystemMessage(content);
        }
        if ("assistant".equalsIgnoreCase(normalized)) {
            return new AssistantMessage(content);
        }
        if ("tool".equalsIgnoreCase(normalized)) {
            return new ToolResponseMessage(List.of(
                    new ToolResponseMessage.ToolResponse("tool-response", "tool", content)
            ));
        }
        return new UserMessage(content);
    }

    private String buildTitle(String conversationId) {
        if (conversationId.length() <= 32) {
            return "chat-" + conversationId;
        }
        return "chat-" + conversationId.substring(0, 32);
    }
}