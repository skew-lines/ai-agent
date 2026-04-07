package com.yl.rag;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgDistanceType.COSINE_DISTANCE;
import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIndexType.HNSW;

/**
 * PgVector 向量存储配置类
 *
 * <p>作用：
 * 配置基于 PostgreSQL + pgvector 的向量数据库，用于存储文档的向量表示（embedding），
 * 支持后续的语义检索（similarity search），是 RAG 系统中的核心组件之一。</p>
 *
 * <p>在 RAG 流程中的位置：</p>
 * <pre>
 * 文档加载 → 文本切分 → 关键词增强 → 向量化（Embedding） → 【PgVector 存储】 → 检索 → 生成
 * </pre>
 *
 * <p>核心能力：</p>
 * <ul>
 *     <li>将文本内容转为向量（embedding）并持久化</li>
 *     <li>支持基于向量距离的相似度搜索</li>
 *     <li>结合 metadata 实现结构化过滤（Hybrid Search）</li>
 * </ul>
 *
 * <p>底层依赖：</p>
 * <ul>
 *     <li>PostgreSQL 数据库</li>
 *     <li>pgvector 扩展（支持向量类型和相似度计算）</li>
 * </ul>
 */
@Configuration
public class PgVectorVectorStoreConfig {

    /**
     * 配置 PgVector 向量存储 Bean
     *
     * @param jdbcTemplate               用于执行 SQL（底层通过它操作 PostgreSQL）
     * @param dashscopeEmbeddingModel    向量模型（用于将文本转为 embedding 向量）
     * @return VectorStore               向量存储实例
     */
    @Bean
    public VectorStore pgVectorVectorStore(JdbcTemplate jdbcTemplate,
                                           EmbeddingModel dashscopeEmbeddingModel) {

        return PgVectorStore.builder(jdbcTemplate, dashscopeEmbeddingModel)

                /**
                 * 向量维度（Embedding 维度）
                 *
                 * 必须与 embedding 模型输出一致，否则会报错。
                 * 例如：
                 * - OpenAI text-embedding-3-small → 1536
                 * - 不一致会导致：vector dimension mismatch
                 */
                .dimensions(1536)

                /**
                 * 距离计算方式（相似度算法）
                 *
                 * COSINE_DISTANCE（余弦距离）：
                 * - 最常用
                 * - 适用于语义相似度计算
                 *
                 * PostgreSQL 对应操作符：
                 * embedding <=> query_vector
                 *
                 * 其他可选：
                 * - EUCLIDEAN_DISTANCE（欧氏距离）
                 * - INNER_PRODUCT（内积）
                 */
                .distanceType(COSINE_DISTANCE)

                /**
                 * 向量索引类型（影响查询性能）
                 *
                 * HNSW（Hierarchical Navigable Small World）：
                 * - 近似最近邻搜索（ANN）
                 * - 查询速度快（适合大规模数据）
                 * - 内存占用较高
                 *
                 * 适用于：
                 * - 百万级向量检索
                 * - 实时语义搜索
                 *
                 * 其他类型：
                 * - IVFFlat（需要训练，适合静态数据）
                 */
                .indexType(HNSW)

                /**
                 * 是否自动初始化表结构（建表）
                 *
                 * false：
                 * - 不自动建表
                 * - 需要手动创建表
                 * - 表结构必须符合框架约定：
                 *
                 * CREATE TABLE vector_store (
                 *     id UUID PRIMARY KEY,
                 *     content TEXT,
                 *     metadata JSONB,
                 *     embedding VECTOR(1536)
                 * );
                 *
                 * true：
                 * - Spring AI 启动时自动建表
                 */
                .initializeSchema(false)

                /**
                 * PostgreSQL schema 名称
                 *
                 * 默认是 public
                 * 如果你使用多 schema，可以改为：
                 * - ai
                 * - rag
                 */
                .schemaName("public")

                /**
                 * 向量表名称
                 *
                 * 默认一般是 vector_store
                 * 可以自定义，例如：
                 * - knowledge_base
                 * - document_vectors
                 */
                .vectorTableName("vector_store")

                /**
                 * 批量处理文档数量（批量插入优化）
                 *
                 * 控制每次向数据库写入的最大文档数：
                 * - 提高吞吐量（减少 SQL 次数）
                 * - 避免单次 SQL 过大导致内存问题
                 *
                 * 注意：
                 * - embedding 通常也会分批（例如 25 条一批）
                 * - 这里是“数据库写入批次”
                 */
                .maxDocumentBatchSize(10000)

                /**
                 * 构建 PgVectorStore 实例
                 */
                .build();
    }
}