package com.yl.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class KnowledgeBaseIndexService {

    private final LoveAppDocumentLoader loveAppDocumentLoader;
    private final MyKeywordEnricher myKeywordEnricher;
    private final VectorStore pgVectorVectorStore;
    private final JdbcTemplate jdbcTemplate;

    public KnowledgeBaseIndexService(LoveAppDocumentLoader loveAppDocumentLoader,
                                     MyKeywordEnricher myKeywordEnricher,
                                     @Qualifier("pgVectorVectorStore") VectorStore pgVectorVectorStore,
                                     JdbcTemplate jdbcTemplate) {
        this.loveAppDocumentLoader = loveAppDocumentLoader;
        this.myKeywordEnricher = myKeywordEnricher;
        this.pgVectorVectorStore = pgVectorVectorStore;
        this.jdbcTemplate = jdbcTemplate;
    }

    public int rebuildPgVectorStore(boolean enrichKeywords) {
        List<Document> documentList = loveAppDocumentLoader.loadMarkdowns();
        List<Document> documentsToIndex = documentList;
        if (enrichKeywords) {
            documentsToIndex = myKeywordEnricher.enrichDocuments(documentList);
        }
        jdbcTemplate.update("delete from public.vector_store");
        pgVectorVectorStore.add(documentsToIndex);
        log.info("Indexed {} documents into pgvector", documentsToIndex.size());
        return documentsToIndex.size();
    }
}
