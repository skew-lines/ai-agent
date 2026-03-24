package com.yl.agent;


import com.yl.advisor.MyLoggerAdvisor;
import com.yl.persistence.AgentPersistenceService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Component;

@Component
public class OpenManus extends ToolCallAgent {

    public OpenManus(ToolCallback[] allTools,
                     ToolCallbackProvider toolCallbackProvider,
                     ChatModel dashscopeChatModel,
                     AgentPersistenceService agentPersistenceService) {
        super(allTools, toolCallbackProvider);
        this.setName("yuManus");
        this.setAgentPersistenceService(agentPersistenceService);
        String SYSTEM_PROMPT = """
                You are YuManus, an all-capable AI assistant, aimed at solving any task presented by the user.
                You have various tools at your disposal that you can call upon to efficiently complete complex requests.
                The current runtime environment is Windows. If you need terminal commands, use Windows cmd-compatible commands such as dir, cd, type, where, or powershell-compatible syntax instead of Unix commands like ls -la.
                """;
        this.setSystemPrompt(SYSTEM_PROMPT);
        String NEXT_STEP_PROMPT = """
                Based on user needs, proactively select the most appropriate tool or combination of tools.
                For complex tasks, you can break down the problem and use different tools step by step to solve it.
                After using each tool, clearly explain the execution results and suggest the next steps.
                If you want to stop the interaction at any point, use the `terminate` tool/function call.
                """;
        this.setNextStepPrompt(NEXT_STEP_PROMPT);
        this.setMaxSteps(20);
        ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
        this.setChatClient(chatClient);
    }
}
