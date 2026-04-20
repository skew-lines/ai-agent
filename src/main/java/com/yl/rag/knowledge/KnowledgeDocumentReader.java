package com.yl.rag.knowledge;

import org.springframework.ai.document.Document;
import org.springframework.core.io.Resource;

import java.util.List;

//统一 Reader 适配接口，扩充Reader类
public interface KnowledgeDocumentReader {

    boolean supports(KnowledgeFileType fileType);

    List<Document> read(Resource resource);
}

