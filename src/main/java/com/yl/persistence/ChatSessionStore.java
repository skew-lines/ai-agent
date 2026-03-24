package com.yl.persistence;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Component
public class ChatSessionStore {

    private final JdbcTemplate jdbcTemplate;

    public ChatSessionStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long findSessionId(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return null;
        }

        // 所有类似聊天的流程共享的查找机制。普通聊天、RAG 聊天以及agent 运行都可以绑定到同一个会话记录。
        return jdbcTemplate.query("""
                        select id
                        from chat_session
                        where session_id = ?
                        limit 1
                        """,
                rs -> rs.next() ? rs.getLong("id") : null,
                conversationId
        );
    }

    public Long getOrCreateSession(String conversationId, String appType, String title) {
        Long sessionPk = findSessionId(conversationId);
        if (sessionPk != null) {
            return sessionPk;
        }
        // 懒创建（按需创建），这样上层只需要一个 chatId，不需要关心会话记录是否已经存在。
        Timestamp now = now();
        jdbcTemplate.update("""
                        insert into chat_session(session_id, app_type, title, session_status, last_message_at, created_at, updated_at)
                        values (?, ?, ?, ?, ?, ?, ?)
                        """,
                conversationId,
                appType,
                title,
                "ACTIVE",
                now,
                now,
                now
        );
        return findSessionId(conversationId);
    }

    public void saveMessage(Long sessionPk, Message message) {
        // 持久化一个简化的消息视图，用于历史回放。工具消息会被扁平化为文本，从而使数据表更易于查询和查看。
        String role = message.getMessageType().getValue();
        String content = extractContent(message);
        String messageType = message.getMessageType().getValue();
        jdbcTemplate.update("""
                        insert into chat_message(session_id, role, content, message_type, created_at)
                        values (?, ?, ?, ?, ?)
                        """,
                sessionPk,
                role,
                content,
                messageType,
                now()
        );
    }

    public void touchSession(Long sessionPk) {
        // 保持会话时间戳为最新，以便最近的对话能够更好地排序。。
        Timestamp now = now();
        jdbcTemplate.update("""
                        update chat_session
                        set last_message_at = ?, updated_at = ?
                        where id = ?
                        """,
                now,
                now,
                sessionPk
        );
    }

    public void deleteSession(Long sessionPk) {
        jdbcTemplate.update("delete from chat_message where session_id = ?", sessionPk);
        jdbcTemplate.update("delete from chat_session where id = ?", sessionPk);
    }

    private String extractContent(Message message) {
        if (message instanceof ToolResponseMessage toolResponseMessage) {
            return toolResponseMessage.getResponses().stream()
                    .map(ToolResponseMessage.ToolResponse::responseData)
                    .reduce((left, right) -> left + "\n" + right)
                    .orElse("");
        }
        return message.getText();
    }

    private Timestamp now() {
        return Timestamp.valueOf(LocalDateTime.now());
    }
}