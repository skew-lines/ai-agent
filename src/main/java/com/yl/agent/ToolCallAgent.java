package com.yl.agent;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.yl.agent.model.AgentState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class ToolCallAgent extends ReActAgent {

    private final ToolCallback[] availableTools;
    private final ToolCallbackProvider toolCallbackProvider;
    private ChatResponse toolCallChatResponse;
    private final ToolCallingManager toolCallingManager;
    private final ChatOptions chatOptions;
    private String lastThought;
    private String lastActionName;
    private String lastActionInputJson;
    private boolean toolsLogged;

    public ToolCallAgent(ToolCallback[] availableTools) {
        this(availableTools, null);
    }

    public ToolCallAgent(ToolCallback[] availableTools, ToolCallbackProvider toolCallbackProvider) {
        super();
        this.availableTools = availableTools;
        this.toolCallbackProvider = toolCallbackProvider;
        this.toolCallingManager = ToolCallingManager.builder().build();
        this.chatOptions = DashScopeChatOptions.builder()
                .withInternalToolExecutionEnabled(false)
                .build();
    }

    @Override
    public boolean think() {
        logAvailableToolsIfNeeded();
        resetStepSnapshot();
        if (StrUtil.isNotBlank(getNextStepPrompt())) {
            UserMessage userMessage = new UserMessage(getNextStepPrompt());
            getMessageList().add(userMessage);
        }
        List<Message> messageList = getMessageList();
        Prompt prompt = new Prompt(messageList, this.chatOptions);
        try {
            var promptCallSpec = getChatClient().prompt(prompt)
                    .system(getSystemPrompt())
                    .toolCallbacks(availableTools);
            if (toolCallbackProvider != null) {
                promptCallSpec = promptCallSpec.toolCallbacks(toolCallbackProvider);
            }
            ChatResponse chatResponse = promptCallSpec.call().chatResponse();
            this.toolCallChatResponse = chatResponse;

            AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
            List<AssistantMessage.ToolCall> toolCallList = assistantMessage.getToolCalls();
            String result = assistantMessage.getText();
            this.lastThought = result;
            log.info("{} thought: {}", getName(), result);
            log.info("{} selected {} tool(s)", getName(), toolCallList.size());

            if (toolCallList.isEmpty()) {
                getMessageList().add(assistantMessage);
                return false;
            }

            this.lastActionName = toolCallList.stream()
                    .map(AssistantMessage.ToolCall::name)
                    .collect(Collectors.joining(","));
            this.lastActionInputJson = JSONUtil.toJsonStr(toolCallList.stream()
                    .map(toolCall -> Map.of(
                            "name", toolCall.name(),
                            "arguments", toolCall.arguments()
                    ))
                    .toList());
            String toolCallInfo = toolCallList.stream()
                    .map(toolCall -> String.format("tool=%s, args=%s", toolCall.name(), toolCall.arguments()))
                    .collect(Collectors.joining("\n"));
            log.info(toolCallInfo);
            return true;
        } catch (Exception e) {
            log.error("{} think stage failed: {}", getName(), e.getMessage());
            this.lastThought = "Think stage error: " + e.getMessage();
            getMessageList().add(new AssistantMessage("Error: " + e.getMessage()));
            return false;
        }
    }

    @Override
    public String act() {
        if (!toolCallChatResponse.hasToolCalls()) {
            return "No tool call required";
        }
        Prompt prompt = new Prompt(getMessageList(), this.chatOptions);
        ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, toolCallChatResponse);
        setMessageList(toolExecutionResult.conversationHistory());
        ToolResponseMessage toolResponseMessage = (ToolResponseMessage) CollUtil.getLast(toolExecutionResult.conversationHistory());
        boolean terminateToolCalled = toolResponseMessage.getResponses().stream()
                .anyMatch(response -> response.name().equals("doTerminate"));
        if (terminateToolCalled) {
            setState(AgentState.FINISHED);
        }
        toolResponseMessage.getResponses().forEach(response -> {
            if (getAgentPersistenceService() != null) {
                getAgentPersistenceService().saveArtifact(
                        getPersistedTaskId(),
                        getConversationId(),
                        response.name(),
                        response.responseData()
                );
            }
        });
        String results = toolResponseMessage.getResponses().stream()
                .map(response -> "Tool " + response.name() + " output: " + response.responseData())
                .collect(Collectors.joining("\n"));
        log.info(results);
        return results;
    }

    @Override
    protected String getStepThought() {
        return lastThought;
    }

    @Override
    protected String getStepActionName() {
        return lastActionName;
    }

    @Override
    protected String getStepActionInputJson() {
        return lastActionInputJson;
    }

    private void resetStepSnapshot() {
        this.lastThought = null;
        this.lastActionName = null;
        this.lastActionInputJson = null;
    }

    private void logAvailableToolsIfNeeded() {
        if (toolsLogged) {
            return;
        }
        toolsLogged = true;
        String localToolInfo = Arrays.stream(availableTools)
                .map(tool -> tool == null ? "null" : tool.toString())
                .collect(Collectors.joining("\n"));
        log.info("{} local tools:\n{}", getName(), localToolInfo);
        log.info("{} tool callback provider: {}", getName(),
                toolCallbackProvider == null ? "null" : toolCallbackProvider.getClass().getName());
        if (toolCallbackProvider != null) {
            String providerToolInfo = Arrays.stream(toolCallbackProvider.getToolCallbacks())
                    .map(tool -> tool == null ? "null" : tool.toString())
                    .collect(Collectors.joining("\n"));
            log.info("{} provider tools:\n{}", getName(), providerToolInfo);
        }
    }
}
