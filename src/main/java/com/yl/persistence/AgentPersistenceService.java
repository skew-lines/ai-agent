package com.yl.persistence;

import cn.hutool.json.JSONUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AgentPersistenceService {

    private static final DateTimeFormatter TASK_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Pattern FILE_PATH_PATTERN = Pattern.compile("(?i)(?:to: )([A-Za-z]:\\\\[^\\r\\n]+|[A-Za-z]:/[^\\r\\n]+)$");

    private final JdbcTemplate jdbcTemplate;
    private final ChatSessionStore chatSessionStore;

    public AgentPersistenceService(JdbcTemplate jdbcTemplate, ChatSessionStore chatSessionStore) {
        this.jdbcTemplate = jdbcTemplate;
        this.chatSessionStore = chatSessionStore;
    }

    public Long createTask(String agentName, String userPrompt, int maxSteps, String conversationId) {
        Long sessionPk = null;
        if (conversationId != null && !conversationId.isBlank()) {
            // 复用同一会话存储，这样当调用方提供 chatId 时，agent 运行可以关联到已有的聊天线程。
            sessionPk = chatSessionStore.getOrCreateSession(conversationId, "agent", buildTitle(agentName, conversationId));
            chatSessionStore.touchSession(sessionPk);
        }
        String taskNo = "agent-" + LocalDateTime.now().format(TASK_NO_FORMATTER) + "-"
                + UUID.randomUUID().toString().substring(0, 8);
        return jdbcTemplate.queryForObject("""
                        insert into agent_task(task_no, session_id, agent_name, user_prompt, state, max_steps, current_step, started_at, created_at)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        returning id
                        """,
                Long.class,
                taskNo,
                sessionPk,
                agentName,
                userPrompt,
                "RUNNING",
                maxSteps,
                0,
                now(),
                now()
        );
    }

    public void updateTaskProgress(Long taskId, int currentStep, String state) {
        if (taskId == null) {
            return;
        }
        jdbcTemplate.update("""
                        update agent_task
                        set current_step = ?, state = ?
                        where id = ?
                        """,
                currentStep,
                state,
                taskId
        );
    }

    public void completeTask(Long taskId, int currentStep, String finalResult) {
        if (taskId == null) {
            return;
        }
        jdbcTemplate.update("""
                        update agent_task
                        set current_step = ?, state = ?, final_result = ?, finished_at = ?
                        where id = ?
                        """,
                currentStep,
                "FINISHED",
                finalResult,
                now(),
                taskId
        );
    }

    public void failTask(Long taskId, int currentStep, String errorMessage) {
        if (taskId == null) {
            return;
        }
        jdbcTemplate.update("""
                        update agent_task
                        set current_step = ?, state = ?, error_message = ?, finished_at = ?
                        where id = ?
                        """,
                currentStep,
                "ERROR",
                errorMessage,
                now(),
                taskId
        );
    }

    public void saveStep(Long taskId, int stepNo, String thought, String actionName, String actionInputJson,
                         String actionOutput, String stepState) {
        if (taskId == null) {
            return;
        }
        // 每个 agent 步骤都会保留推理快照和动作元数据，这样我们就可以在运行结束后还原当时发生了什么。
        jdbcTemplate.update("""
                        insert into agent_step(task_id, step_no, thought, action_name, action_input, action_output, step_state, created_at)
                        values (?, ?, ?, ?, cast(? as jsonb), ?, ?, ?)
                        on conflict (task_id, step_no) do update
                        set thought = excluded.thought,
                            action_name = excluded.action_name,
                            action_input = excluded.action_input,
                            action_output = excluded.action_output,
                            step_state = excluded.step_state
                        """,
                taskId,
                stepNo,
                thought,
                actionName,
                normalizeJson(actionInputJson),
                actionOutput,
                stepState,
                now()
        );
    }

    public void saveArtifact(Long taskId, String conversationId, String toolName, String toolResult) {
        if (taskId == null || toolResult == null || toolResult.isBlank()) {
            return;
        }
        // 只有当工具输出看起来像文件路径时，才会写入工具产物，这样可以让这张表专注于真正生成的文件。
        String filePath = extractFilePath(toolResult);
        if (filePath == null) {
            return;
        }
        Path path = Path.of(filePath);
        Long sessionPk = chatSessionStore.findSessionId(conversationId);
        String fileName = path.getFileName() == null ? null : path.getFileName().toString();
        String fileType = detectFileType(fileName);
        Long fileSize = null;
        try {
            if (Files.exists(path)) {
                fileSize = Files.size(path);
            }
        } catch (Exception ignored) {
        }
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("toolResult", toolResult);
        extra.put("exists", Files.exists(path));
        jdbcTemplate.update("""
                        insert into tool_artifact(task_id, session_id, tool_name, file_name, file_path, file_type, file_size, extra, created_at)
                        values (?, ?, ?, ?, ?, ?, ?, cast(? as jsonb), ?)
                        """,
                taskId,
                sessionPk,
                toolName,
                fileName,
                filePath,
                fileType,
                fileSize,
                JSONUtil.toJsonStr(extra),
                now()
        );
    }

    private String extractFilePath(String toolResult) {
        Matcher matcher = FILE_PATH_PATTERN.matcher(toolResult.trim());
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1).replace('/', '\\');
    }

    private String detectFileType(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return null;
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }

    private String normalizeJson(String json) {
        if (json == null || json.isBlank()) {
            return "{}";
        }
        return json;
    }

    private String buildTitle(String agentName, String conversationId) {
        String prefix = agentName == null || agentName.isBlank() ? "agent" : agentName;
        return prefix + "-" + conversationId;
    }

    private Timestamp now() {
        return Timestamp.valueOf(LocalDateTime.now());
    }
}