package com.yl.rag.clean;

import org.springframework.ai.document.Document;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 默认文档清洗器
 *
 * 作用：
 * 在 RAG（Retrieval-Augmented Generation）流程中，对切分后的 Document 进行清洗，
 * 去除噪声、统一格式、去重，从而提升 embedding 质量与检索效果。
 */
public class DefaultDocumentCleaner implements DocumentCleaner {

    @Override
    public List<Document> clean(List<Document> documents) {
        // 最终返回的清洗结果
        List<Document> result = new ArrayList<>();

        // 用于去重（避免重复内容进入向量库）
        Set<String> seen = new HashSet<>();

        for (Document doc : documents) {
            // 原始文本内容
            String original = doc.getText();

            // 1️⃣ 过滤空内容（无意义数据直接丢弃）
            if (original == null || original.isBlank()) {
                continue;
            }

            // 获取元信息（用于区分内容类型，比如代码块 / 普通文本）
            Map<String, Object> metadata = doc.getMetadata();
            String category = metadata == null ? null : (String) metadata.get("category");

            String cleanedText;

            /**
             * 2️⃣ 分类处理
             *
             * - 代码块：保留原始结构（避免破坏语义）
             * - 普通文本：做规范化处理（减少 embedding 噪声）
             */
            if ("code_block".equals(category) || "code_inline".equals(category)) {
                // 代码不做复杂清洗，只去掉首尾空白
                cleanedText = original.trim();
            } else {
                // 普通文本进行标准化处理
                cleanedText = normalizeText(original);
            }

            // 3️⃣ 再次过滤空文本（清洗后可能变空）
            if (cleanedText.isBlank()) {
                continue;
            }

            // 4️⃣ 过滤过短文本（信息密度太低，不利于向量检索）
            // 比如：单个词、符号等
            if (cleanedText.length() < 5) {
                continue;
            }

            // 5️⃣ 去重（避免重复内容污染向量库）
            if (!seen.add(cleanedText)) {
                continue;
            }

            /**
             * 6️⃣ 构建新的 Document
             *
             * - 保留原 metadata
             * - 添加标记：cleaned = true（方便后续调试或链路追踪）
             */
            Document cleanedDoc = Document.builder()
                    .text(cleanedText)
                    .metadata(metadata)
                    .metadata("cleaned", true)
                    .build();

            result.add(cleanedDoc);
        }

        return result;
    }

    /**
     * 文本规范化处理
     *
     * 目标：统一格式，减少 embedding 噪声，提高语义一致性
     */
    private String normalizeText(String text) {
        return text
                // 统一换行符（兼容 Windows \r\n → \n）
                .replace("\r", "\n")

                // 多个空格 / tab → 单个空格（避免无意义差异）
                .replaceAll("[ \\t]+", " ")

                // 连续 3 个以上换行 → 保留 2 个（段落分隔）
                .replaceAll("\\n{3,}", "\n\n")

                // 去除首尾空白
                .trim();
    }
}