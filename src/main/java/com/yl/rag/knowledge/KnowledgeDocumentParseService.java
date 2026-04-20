package com.yl.rag.knowledge;

import org.springframework.ai.document.Document;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

//统一解析服务
@Service
public class KnowledgeDocumentParseService {

    private final KnowledgeDocumentReaderFactory readerFactory;

    public KnowledgeDocumentParseService(KnowledgeDocumentReaderFactory readerFactory) {
        this.readerFactory = readerFactory;
    }

    public List<Document> parse(String fileName, Resource resource) {
        KnowledgeFileType fileType = KnowledgeFileType.fromFileName(fileName);
        KnowledgeDocumentReader reader = readerFactory.getReader(fileType);
        return reader.read(resource);
    }
}

