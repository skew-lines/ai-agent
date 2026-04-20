package com.yl.rag.knowledge;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class MarkdownKnowledgeDocumentReader implements KnowledgeDocumentReader {

    @Override
    public boolean supports(KnowledgeFileType fileType) {
        return fileType == KnowledgeFileType.MARKDOWN;
    }

    @Override
    public List<Document> read(Resource resource) {
        // 动态获取当前资源的文件名
        String fileName = resource.getFilename();

        // 当前文件对应的 metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "markdown");
        metadata.put("language", "zh-en");
        metadata.put("fileName", fileName);

        // 每次读取时动态构建 config，避免 fileName 无法注入的问题
        MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                .withIncludeCodeBlock(true)
                .withIncludeBlockquote(false)
                .withHorizontalRuleCreateDocument(true)
                .withAdditionalMetadata(metadata)
                .build();

        MarkdownDocumentReader reader = new MarkdownDocumentReader(resource, config);
        return reader.get();
    }
}