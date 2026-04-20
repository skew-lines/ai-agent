package com.yl.rag.clean;


import org.springframework.ai.document.Document;

import java.util.List;
//文档清洗
public interface DocumentCleaner {
    List<Document> clean(List<Document> documents);
}
