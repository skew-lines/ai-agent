package com.yl.agent;

import cn.hutool.core.util.StrUtil;
import org.springframework.ai.chat.messages.UserMessage;
import com.yl.agent.model.AgentState;
import com.yl.persistence.AgentPersistenceService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.ai.chat.messages.Message;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Data
@Slf4j
/**
 * Agent 抽象基类
 * 封装了 Agent 的执行流程（循环 step）、状态管理、流式输出和持久化能力
 */
public abstract class BaseAgent {

    // Agent 名称（用于持久化 / 日志）
    private String name;

    // 系统提示词（给大模型用）
    private String systemPrompt;

    // 下一步提示词（用于 step 推进）
    private String nextStepPrompt;

    // 当前 Agent 状态（空闲 / 运行中 / 完成 / 错误）
    private AgentState state = AgentState.IDLE;

    // 当前执行到第几步
    private int currentStep = 0;

    // 最大允许执行步数（防止死循环）
    private int maxSteps = 10;

    // AI 聊天客户端（用于调用大模型）
    private ChatClient chatClient;

    // 消息上下文（用于多轮推理）
    private List<Message> messageList = new ArrayList<>();

    // 持久化服务（可选，用于记录任务和步骤）
    private AgentPersistenceService agentPersistenceService;

    // 持久化后的任务 ID
    private Long persistedTaskId;

    // 会话 ID（用于关联聊天线程）
    private String conversationId;

    /**
     * 同步执行入口（不传 conversationId）
     */
    public String run(String userPrompt) {
        return run(userPrompt, null);
    }

    /**
     * 同步执行 Agent 主流程
     *
     * @param userPrompt 用户输入
     * @param conversationId 会话 ID（可选）
     * @return 最终执行结果
     */
    public String run(String userPrompt, String conversationId) {
        // 运行前校验（状态必须是 IDLE，输入不能为空）
        validateBeforeRun(userPrompt);

        // 初始化运行状态
        this.state = AgentState.RUNNING;
        this.conversationId = conversationId;

        // 将用户输入加入消息上下文
        this.messageList.add(new UserMessage(userPrompt));

        // 如果开启持久化，则创建任务
        createTaskIfNeeded(userPrompt);

        List<String> results = new ArrayList<>();
        try {
            // 主循环：逐步执行 Agent
            for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {
                int stepNumber = i + 1;
                currentStep = stepNumber;

                // 更新任务进度
                updateTaskProgress("RUNNING");

                log.info("Executing step {}/{}", stepNumber, maxSteps);

                // 执行单步逻辑（由子类实现）
                String stepResult = step();

                // 记录结果
                String result = "Step " + stepNumber + ": " + stepResult;
                results.add(result);

                // 持久化当前步骤（包含最终执行结果）
                persistStep(stepResult, "FINISHED");
            }

            // 如果达到最大步数仍未结束，则强制终止
            if (currentStep >= maxSteps && state != AgentState.FINISHED) {
                state = AgentState.FINISHED;
                results.add("Terminated: Reached max steps (" + maxSteps + ")");
            }

            // 拼接最终结果
            String finalResult = String.join("\n", results);

            // 标记任务完成
            finishTask(finalResult);

            return finalResult;

        } catch (Exception e) {
            // 异常处理：标记失败并记录
            state = AgentState.ERROR;
            log.error("error executing agent", e);
            failTask(e.getMessage());

            return "Execution error: " + e.getMessage();

        } finally {
            // 无论成功失败都清理上下文
            cleanup();
        }
    }

    /**
     * 流式执行入口（不传 conversationId）
     */
    public SseEmitter runStream(String userPrompt) {
        return runStream(userPrompt, null);
    }

    /**
     * 流式执行 Agent（通过 SSE 实时返回每一步结果）
     */
    public SseEmitter runStream(String userPrompt, String conversationId) {
        // SSE 超时时间 5 分钟
        SseEmitter sseEmitter = new SseEmitter(300000L);

        // 异步执行 Agent，避免阻塞请求线程
        CompletableFuture.runAsync(() -> {
            try {
                validateBeforeRun(userPrompt);

                this.state = AgentState.RUNNING;
                this.conversationId = conversationId;
                this.messageList.add(new UserMessage(userPrompt));

                createTaskIfNeeded(userPrompt);

                List<String> results = new ArrayList<>();

                // 循环执行每一步
                for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {
                    int stepNumber = i + 1;
                    currentStep = stepNumber;

                    updateTaskProgress("RUNNING");

                    log.info("Executing step {}/{}", stepNumber, maxSteps);

                    String stepResult = step();

                    String result = "Step " + stepNumber + ": " + stepResult;
                    results.add(result);

                    // 持久化步骤
                    persistStep(stepResult, "FINISHED");

                    // 实时推送给前端
                    sseEmitter.send(result);
                }

                // 超过最大步数时终止
                if (currentStep >= maxSteps && state != AgentState.FINISHED) {
                    state = AgentState.FINISHED;
                    String result = "Terminated: Reached max steps (" + maxSteps + ")";
                    results.add(result);
                    sseEmitter.send(result);
                }

                // 完成任务
                finishTask(String.join("\n", results));

                // 通知前端结束
                sseEmitter.complete();

            } catch (Exception e) {
                // 异常处理
                state = AgentState.ERROR;
                log.error("error executing agent", e);
                failTask(e.getMessage());

                try {
                    sseEmitter.send("Execution error: " + e.getMessage());
                    sseEmitter.complete();
                } catch (IOException ex) {
                    sseEmitter.completeWithError(ex);
                }
            } finally {
                cleanup();
            }
        });

        // SSE 超时处理
        sseEmitter.onTimeout(() -> {
            this.state = AgentState.ERROR;
            failTask("SSE connection timeout");
            cleanup();
            log.warn("SSE connection timeout");
        });

        // SSE 正常结束回调
        sseEmitter.onCompletion(() -> log.info("SSE connection completed"));

        return sseEmitter;
    }

    /**
     * 抽象方法：定义每一步执行逻辑
     * 子类必须实现
     */
    public abstract String step();

    /**
     * 当前步骤的思考内容（可选，用于持久化）
     */
    protected String getStepThought() {
        return null;
    }

    /**
     * 当前步骤的动作名称（可选）
     */
    protected String getStepActionName() {
        return null;
    }

    /**
     * 当前步骤的动作输入参数（JSON）（可选）
     */
    protected String getStepActionInputJson() {
        return null;
    }

    /**
     * 清理运行时状态（避免影响下一次执行）
     */
    protected void cleanup() {
        this.messageList = new ArrayList<>();
        this.currentStep = 0;
        this.persistedTaskId = null;
        this.conversationId = null;
        this.state = AgentState.IDLE;
    }

    /**
     * 运行前校验
     */
    private void validateBeforeRun(String userPrompt) {
        // 只能从 IDLE 状态启动
        if (this.state != AgentState.IDLE) {
            throw new RuntimeException("Cannot run agent from state: " + this.state);
        }

        // 用户输入不能为空
        if (StrUtil.isBlank(userPrompt)) {
            throw new RuntimeException("Cannot run agent with empty user prompt");
        }
    }

    /**
     * 创建任务（如果启用了持久化）
     */
    private void createTaskIfNeeded(String userPrompt) {
        if (agentPersistenceService == null) {
            return;
        }
        this.persistedTaskId = agentPersistenceService.createTask(name, userPrompt, maxSteps, conversationId);
    }

    /**
     * 更新任务进度（当前执行到第几步）
     */
    private void updateTaskProgress(String taskState) {
        if (agentPersistenceService == null || persistedTaskId == null) {
            return;
        }
        agentPersistenceService.updateTaskProgress(persistedTaskId, currentStep, taskState);
    }

    /**
     * 持久化单个步骤信息
     */
    private void persistStep(String stepResult, String stepState) {
        if (agentPersistenceService == null || persistedTaskId == null) {
            return;
        }
        agentPersistenceService.saveStep(
                persistedTaskId,
                currentStep,
                getStepThought(),
                getStepActionName(),
                getStepActionInputJson(),
                stepResult,
                stepState
        );
    }

    /**
     * 标记任务完成
     */
    private void finishTask(String finalResult) {
        if (agentPersistenceService == null || persistedTaskId == null) {
            return;
        }
        if (state != AgentState.ERROR) {
            state = AgentState.FINISHED;
        }
        agentPersistenceService.completeTask(persistedTaskId, currentStep, finalResult);
    }

    /**
     * 标记任务失败
     */
    private void failTask(String errorMessage) {
        if (agentPersistenceService == null || persistedTaskId == null) {
            return;
        }
        agentPersistenceService.failTask(persistedTaskId, currentStep, errorMessage);
    }
}