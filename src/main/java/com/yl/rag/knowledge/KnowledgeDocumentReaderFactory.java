package com.yl.rag.knowledge;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

//工厂
@Component
public class KnowledgeDocumentReaderFactory {

    private final List<KnowledgeDocumentReader> readers;

    public KnowledgeDocumentReaderFactory(List<KnowledgeDocumentReader> readers) {
        this.readers = readers;
    }

    public KnowledgeDocumentReader getReader(KnowledgeFileType fileType) {
        return readers.stream()
                .filter(reader -> reader.supports(fileType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No reader found for fileType: " + fileType));
    }
}

