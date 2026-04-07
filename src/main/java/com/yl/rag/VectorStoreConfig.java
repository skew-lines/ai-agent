package com.yl.rag;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * 恋爱大师向量库配置（初始化基于内存的向量存储 Bean）
 */
/*@Configuration*/
@Slf4j
public class VectorStoreConfig {

    @Resource
    private DocumentLoader loveAppDocumentLoader;

    @Resource
    private TokenTextSplitter TokenTextSplitter;

    @Resource
    private KeywordEnricher KeywordEnricher;

    @Bean
    VectorStore loveAppVectorStore(EmbeddingModel dashscopeEmbeddingModel) {
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(dashscopeEmbeddingModel).build();
        List<Document> documentList = loveAppDocumentLoader.loadMarkdowns();
//        List<Document> splitDocuments = myTokenTextSplitter.splitCustomized(documentList);
        List<Document> documentsToStore = documentList;
        try {
            documentsToStore = KeywordEnricher.enrichDocuments(documentList);
        } catch (Exception e) {
            log.warn("Keyword enrichment failed during vector store initialization, fallback to raw documents", e);
        }
        simpleVectorStore.add(documentsToStore);
        return simpleVectorStore;
    }
}
