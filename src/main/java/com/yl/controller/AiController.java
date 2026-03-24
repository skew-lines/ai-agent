package com.yl.controller;

import com.yl.agent.OpenManus;
import com.yl.app.LoveApp;
import com.yl.persistence.AgentPersistenceService;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private LoveApp loveApp;

    @Resource
    private ToolCallback[] allTools;

    @Resource
    private ChatModel dashscopeChatModel;

    @Resource
    private ToolCallbackProvider toolCallbackProvider;

    @Resource
    private AgentPersistenceService agentPersistenceService;

    @GetMapping("/love_app/chat/sync")
    public String doChatWithLoveAppSync(String message, String chatId) {
        return loveApp.doChat(message, chatId);
    }

    @GetMapping("/love_app/chat/rag")
    public String doChatWithLoveAppRag(String message, String chatId) {
        return loveApp.doChatWithRag(message, chatId);
    }

    @GetMapping(value = "/love_app/chat/sse", produces = "text/event-stream;charset=UTF-8")
    public Flux<String> doChatWithLoveAppSSE(String message, String chatId) {
        return loveApp.doChatByStream(message, chatId);
    }

    @GetMapping(value = "/love_app/chat/server_sent_event", produces = "text/event-stream;charset=UTF-8")
    public Flux<ServerSentEvent<String>> doChatWithLoveAppServerSentEvent(String message, String chatId) {
        return loveApp.doChatByStream(message, chatId)
                .map(chunk -> ServerSentEvent.<String>builder()
                        .data(chunk)
                        .build());
    }

    @GetMapping(value = "/love_app/chat/sse_emitter", produces = "text/event-stream;charset=UTF-8")
    public SseEmitter doChatWithLoveAppServerSseEmitter(String message, String chatId) {
        SseEmitter sseEmitter = new SseEmitter(180000L);
        loveApp.doChatByStream(message, chatId)
                .subscribe(chunk -> {
                    try {
                        sseEmitter.send(SseEmitter.event().data(chunk));
                    } catch (IOException e) {
                        sseEmitter.completeWithError(e);
                    }
                }, sseEmitter::completeWithError, sseEmitter::complete);
        return sseEmitter;
    }

    @GetMapping(value = "/manus/chat", produces = "text/event-stream;charset=UTF-8")
    public SseEmitter doChatWithManus(String message, String chatId) {
        OpenManus openManus = new OpenManus(allTools, toolCallbackProvider, dashscopeChatModel, agentPersistenceService);
        return openManus.runStream(message, chatId);
    }
}
