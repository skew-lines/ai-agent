package com.yl.rag;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.model.transformer.KeywordMetadataEnricher;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 文档关键词元信息增强器（Keyword Metadata Enricher）
 *
 * <p>作用：
 * 基于大语言模型（LLM）对文档内容进行语义分析，自动提取关键词，
 * 并将关键词写入 Document 的 metadata 中，作为结构化信息。</p>
 *
 * <p>在 RAG（Retrieval-Augmented Generation）中的位置：
 * 文档加载 → 文本切分 → 【关键词增强】→ 向量化 → 入库</p>
 *
 * <p>核心价值：</p>
 * <ul>
 *     <li>提升向量检索的语义召回能力（关键词辅助语义理解）</li>
 *     <li>支持元数据过滤（Metadata Filter），实现结构化检索</li>
 *     <li>便于构建混合检索（Hybrid Search = 向量检索 + 关键词检索）</li>
 *     <li>为后续 rerank 提供额外特征信息</li>
 * </ul>
 *
 * <p>实现原理：</p>
 * <ul>
 *     <li>调用 LLM（DashScope）分析文档内容</li>
 *     <li>提取 Top-K 关键词（当前设置为 5 个）</li>
 *     <li>将关键词写入 metadata，例如：keywords 字段</li>
 * </ul>
 *
 * <p>示例：</p>
 * <pre>
 * 原始文档：
 * content: "Redis 是一个高性能的内存数据库"
 *
 * 增强后：
 * content: "Redis 是一个高性能的内存数据库"
 * metadata: {
 *     "keywords": ["Redis", "缓存", "内存数据库", "高性能", "NoSQL"]
 * }
 * </pre>
 *
 * <p>注意：</p>
 * <ul>
 *     <li>LLM 调用存在一定开销，建议在离线构建索引阶段执行</li>
 *     <li>关键词数量（Top-K）可根据业务场景调整</li>
 * </ul>
 */
@Component
public class KeywordEnricher {

    /**
     * DashScope 大模型，用于关键词提取
     */
    @Resource
    private ChatModel dashscopeChatModel;

    /**
     * 对文档列表进行关键词元信息增强
     *
     * @param documents 原始文档列表（仅包含 content）
     * @return 增强后的文档列表（metadata 中新增 keywords 字段）
     */
    public List<Document> enrichDocuments(List<Document> documents) {

        // KeywordMetadataEnricher：
        // Spring AI 提供的文档转换器，用于基于 LLM 自动提取关键词并写入 metadata
        // 参数2：提取关键词数量（Top-K）
        KeywordMetadataEnricher keywordMetadataEnricher =
                new KeywordMetadataEnricher(dashscopeChatModel, 5);

        // apply 方法会：
        // 1. 调用 LLM 分析每个 Document 的 content
        // 2. 提取关键词
        // 3. 写入 metadata（默认字段：keywords）
        return keywordMetadataEnricher.apply(documents);
    }
}