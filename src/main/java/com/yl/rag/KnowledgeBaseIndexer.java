package com.yl.rag;


import com.yl.AiAgentApplication;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

@Slf4j
public class KnowledgeBaseIndexer {

    public static void main(String[] args) {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(AiAgentApplication.class)
                .web(WebApplicationType.NONE)
                .logStartupInfo(false)
                .run(args)) {
            KnowledgeBaseIndexService knowledgeBaseIndexService = context.getBean(KnowledgeBaseIndexService.class);
            int indexedCount = knowledgeBaseIndexService.rebuildPgVectorStore(false);
            log.info("Knowledge base indexing completed, indexedCount={}", indexedCount);
        }
    }
}
