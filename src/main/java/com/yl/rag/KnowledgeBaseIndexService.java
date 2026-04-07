package com.yl.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 知识库索引服务
 *
 * <p>核心职责：
 * 1. 从本地 Markdown 文档中加载知识内容
 * 2. 可选地对文档进行关键词增强
 * 3. 清空旧的向量数据
 * 4. 将文档分批写入 pgvector 向量库
 *
 * <p>主要用途：
 * - 构建 RAG 知识库
 * - 重建向量索引
 * - 为后续相似度检索提供数据基础
 *
 * <p>注意事项：
 * - DashScope embedding 接口单次最多处理 25 条文本
 * - 因此必须对 Document 列表进行分批写入
 */
@Service
@Slf4j
public class KnowledgeBaseIndexService {

    /**
     * 单批次向量化的最大文档数
     *
     * <p>原因：
     * DashScope embedding 接口限制一次最多传入 25 条文本，
     * 如果超过该数量，会抛出异常：
     * The input texts limit 25.
     */
    private static final int EMBEDDING_BATCH_SIZE = 25;

    /**
     * Markdown 文档加载器
     *
     * <p>负责从 classpath:document/*.md 中读取文档，
     * 并转换为 Spring AI 的 Document 对象列表。
     */
    private final DocumentLoader loveAppDocumentLoader;

    /**
     * 关键词增强器
     *
     * <p>作用：
     * 在原始文档基础上补充关键词、标签或额外描述，
     * 提高 embedding 后的召回效果。
     */
    private final KeywordEnricher myKeywordEnricher;

    /**
     * pgvector 向量存储对象
     *
     * <p>负责将 Document 转换为向量后写入 PostgreSQL + pgvector。
     */
    private final VectorStore pgVectorVectorStore;

    /**
     * JDBC 操作模板
     *
     * <p>这里主要用于在重建索引前清空旧表数据。
     */
    private final JdbcTemplate jdbcTemplate;

    public KnowledgeBaseIndexService(DocumentLoader loveAppDocumentLoader,
                                     KeywordEnricher myKeywordEnricher,
                                     @Qualifier("pgVectorVectorStore") VectorStore pgVectorVectorStore,
                                     JdbcTemplate jdbcTemplate) {
        this.loveAppDocumentLoader = loveAppDocumentLoader;
        this.myKeywordEnricher = myKeywordEnricher;
        this.pgVectorVectorStore = pgVectorVectorStore;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 重建 pgvector 向量库
     *
     * @param enrichKeywords 是否启用关键词增强
     *                       true：先增强文档再入库
     *                       false：直接使用原始文档入库
     * @return 实际写入向量库的文档数量
     *
     * <p>执行流程：
     * 1. 加载 Markdown 文档
     * 2. 判断是否进行关键词增强
     * 3. 清空旧向量数据
     * 4. 按批次写入 pgvector
     * 5. 返回写入总数
     *
     * <p>说明：
     * - 该方法适合“全量重建索引”场景
     * - 当前实现是先删后建
     * - 如果中途失败，可能会导致向量库为空或部分数据写入
     */
    public int rebuildPgVectorStore(boolean enrichKeywords) {

        // 1. 加载 Markdown 文档
        List<Document> documentList = loveAppDocumentLoader.loadMarkdowns();

        // 如果没有加载到任何文档，则直接返回
        if (documentList == null || documentList.isEmpty()) {
            log.warn("没有加载到任何 Markdown 文档");
            return 0;
        }

        // 2. 默认使用原始文档进行索引
        List<Document> documentsToIndex = documentList;

        // 如果启用关键词增强，则先增强再写入向量库
        if (enrichKeywords) {
            documentsToIndex = myKeywordEnricher.enrichDocuments(documentList);
        }

        // 增强后再次校验，防止结果为空
        if (documentsToIndex == null || documentsToIndex.isEmpty()) {
            log.warn("没有可写入向量库的文档");
            return 0;
        }

        // 3. 清空旧的向量数据
        jdbcTemplate.update("delete from public.vector_store");
        log.info("已清空 vector_store 表，准备写入 {} 条文档", documentsToIndex.size());

        // 4. 分批写入向量库
        int total = documentsToIndex.size();

        for (int i = 0; i < total; i += EMBEDDING_BATCH_SIZE) {

            // 计算当前批次结束位置
            int end = Math.min(i + EMBEDDING_BATCH_SIZE, total);

            // 截取当前批次文档
            List<Document> batch = documentsToIndex.subList(i, end);

            // 打印批次写入日志，方便排查问题
            log.info("正在写入第 {} 批，文档范围 [{} - {})，本批 {} 条",
                    (i / EMBEDDING_BATCH_SIZE) + 1, i, end, batch.size());

            // 将当前批次写入 pgvector
            pgVectorVectorStore.add(batch);
        }

        // 5. 输出最终结果
        log.info("Indexed {} documents into pgvector", total);
        return total;
    }
}