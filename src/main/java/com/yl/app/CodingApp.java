package com.yl.app;

import com.yl.advisor.MyLoggerAdvisor;
import com.yl.chatmemory.PgChatMemory;
import com.yl.rag.QueryRewriter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

@Component
@Slf4j
public class CodingApp {

    private static final String SYSTEM_PROMPT =
            "你是一个专业的AI编程助手，擅长软件开发、系统设计和问题排查。"
                    + "你的目标是帮助用户高效解决编程问题，并提供清晰、可执行的技术方案。"

                    + "在对话开始时，请主动询问用户的需求背景，例如：编程语言、技术栈、使用框架、运行环境等。"

                    + "在处理用户问题时，你需要遵循以下流程："
                    + "1. 理解问题：分析用户需求或报错信息，必要时主动追问补充细节；"
                    + "2. 问题拆解：将复杂问题拆分为多个子问题；"
                    + "3. 提供方案：给出清晰的解决思路，并说明原因；"
                    + "4. 代码实现：提供规范、可运行的示例代码（必要时附带注释）；"
                    + "5. 优化建议：给出性能优化、架构优化或最佳实践建议；"

                    + "在回答时请遵循以下原则："
                    + "1. 优先给出结论，再解释原因；"
                    + "2. 避免空泛描述，尽量结合代码示例说明；"
                    + "3. 如果存在多种方案，列出优缺点并推荐最佳方案；"
                    + "4. 对于错误或Bug，说明根因并给出修复方式；"
                    + "5. 输出结构清晰，分点说明；"

                    + "如果用户的问题涉及数据库、并发、分布式系统、网络、操作系统等，请结合底层原理进行解释。"

                    + "当信息不足时，不要盲目猜测，应优先向用户提问以获取必要信息。";

    private final ChatClient chatClient;

    public CodingApp(ChatModel dashscopeChatModel, PgChatMemory chatMemory) {
        // 这里把会话记忆实现切到 PostgreSQL。
        // 这样模型每次取上下文时，读的是数据库里的历史消息，而不是进程内存。
        this.chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder((ChatMemory) chatMemory).build(),
                        new MyLoggerAdvisor()
                )
                .build();
    }

    public String doChat(String message, String chatId) {
        // chatId 会作为会话主键传给 ChatMemory，决定当前对话读取和写入哪一组历史消息。
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    public Flux<String> doChatByStream(String message, String chatId) {
        // 流式对话也复用同一套 ChatMemory，因此服务重启后仍然可以续聊。
        return chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .stream()
                .content();
    }

    record CodingReport(String title, List<String> suggestions) {
    }

    public CodingReport doChatWithReport(String message, String chatId) {
        CodingReport codingReport = chatClient
                .prompt()
                .system(SYSTEM_PROMPT + " 每次对话后都要生成一份编程分析报告，标题为“用户专属编程报告”，内容为建议列表 suggestions。"
                        + " suggestions 中应包含针对用户问题的具体改进建议、排查步骤、优化方案或学习建议。")
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .entity(CodingReport.class);

        log.info("codingReport: {}", codingReport);
        return codingReport;
    }

/*    @Resource
    //本地内存RAG向量库
    private VectorStore loveAppVectorStore;

    @Resource
    //云端RAG向量库
    private Advisor loveAppRagCloudAdvisor;*/

    @Resource
    private VectorStore pgVectorVectorStore;

    @Resource
    private QueryRewriter queryRewriter;

    public String doChatWithRag(String message, String chatId) {
        // 先重写查询，再交给 pgvector 做召回，最后把检索结果拼回模型上下文。
        String rewrittenMessage = queryRewriter.doQueryRewrite(message);
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(rewrittenMessage)
                //consumer接口，消费者，给advisors添加一个上下文
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .advisors(new MyLoggerAdvisor())
               //.advisors(new QuestionAnswerAdvisor(loveAppVectorStore))
                //                .advisors(loveAppRagCloudAdvisor)
                .advisors(new QuestionAnswerAdvisor(pgVectorVectorStore))
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    @Resource
    private ToolCallback[] allTools;

    public String doChatWithTools(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .advisors(new MyLoggerAdvisor())
                .toolCallbacks(allTools)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    @Resource
    private ToolCallbackProvider toolCallbackProvider;

    public String doChatWithMcp(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .advisors(new MyLoggerAdvisor())
                .toolCallbacks(toolCallbackProvider)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }
}
