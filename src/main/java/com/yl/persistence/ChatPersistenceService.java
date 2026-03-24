package com.yl.persistence;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatPersistenceService {

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public void saveConversation(String chatId, String userMessage, String assistantMessage) {
        if (StrUtil.hasBlank(chatId, userMessage) || StrUtil.isBlank(assistantMessage)) {
            return;
        }
        Long sessionPk = getOrCreateSession(chatId);
        saveMessage(sessionPk, "user", userMessage);
        saveMessage(sessionPk, "assistant", assistantMessage);
        touchSession(sessionPk);
    }

    @Transactional
    public void saveUserMessage(String chatId, String userMessage) {
        if (StrUtil.hasBlank(chatId, userMessage)) {
            return;
        }
        Long sessionPk = getOrCreateSession(chatId);
        saveMessage(sessionPk, "user", userMessage);
        touchSession(sessionPk);
    }

    @Transactional
    public void saveAssistantMessage(String chatId, String assistantMessage) {
        if (StrUtil.hasBlank(chatId, assistantMessage)) {
            return;
        }
        Long sessionPk = getOrCreateSession(chatId);
        saveMessage(sessionPk, "assistant", assistantMessage);
        touchSession(sessionPk);
    }

    private Long getOrCreateSession(String chatId) {
        Long sessionPk = findSessionId(chatId);
        if (sessionPk != null) {
            return sessionPk;
        }
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("""
                insert into chat_session(session_id, app_type, title, session_status, last_message_at, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?)
                """,
                chatId,
                "love_app",
                buildTitle(chatId),
                "ACTIVE",
                Timestamp.valueOf(now),
                Timestamp.valueOf(now),
                Timestamp.valueOf(now)
        );
        sessionPk = findSessionId(chatId);
        if (sessionPk == null) {
            throw new IllegalStateException("Failed to create chat session for chatId=" + chatId);
        }
        return sessionPk;
    }

    private Long findSessionId(String chatId) {
        return jdbcTemplate.query("""
                        select id
                        from chat_session
                        where session_id = ?
                        limit 1
                        """,
                rs -> rs.next() ? rs.getLong("id") : null,
                chatId
        );
    }

    private void saveMessage(Long sessionPk, String role, String content) {
        jdbcTemplate.update("""
                        insert into chat_message(session_id, role, content, message_type, created_at)
                        values (?, ?, ?, ?, ?)
                        """,
                sessionPk,
                role,
                content,
                "text",
                Timestamp.valueOf(LocalDateTime.now())
        );
    }

    private void touchSession(Long sessionPk) {
        jdbcTemplate.update("""
                        update chat_session
                        set last_message_at = ?, updated_at = ?
                        where id = ?
                        """,
                Timestamp.valueOf(LocalDateTime.now()),
                Timestamp.valueOf(LocalDateTime.now()),
                sessionPk
        );
    }

    private String buildTitle(String chatId) {
        if (chatId.length() <= 32) {
            return "会话-" + chatId;
        }
        return "会话-" + chatId.substring(0, 32);
    }
}
