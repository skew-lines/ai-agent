package com.yl.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 文档加载器（Markdown → Document）
 *
 * <p>核心职责：
 * 1. 从 classpath:document 目录加载所有 Markdown 文件
 * 2. 将 Markdown 解析为 Spring AI 的 Document 对象
 * 3. 为每个 Document 添加 metadata（用于后续向量检索过滤）
 *
 * <p>设计说明：
 * - 支持多文档加载（用于 RAG 知识库构建）
 * - 支持 metadata 注入（如 filename / status）
 * - 支持按 Markdown 结构切分（通过 --- 分段）
 *
 * <p>典型文件命名规范：
 *   编程AI助手_java.md   → status=java
 *   编程AI助手_mysql.md  → status=mysql
 *
 * <p>后续用途：
 * - 用于向量数据库（pgvector）入库
 * - 用于 RAG 检索时 metadata 过滤（如按技术栈筛选）
 */
@Component
@Slf4j
public class DocumentLoader {

    /**
     * Spring 资源解析器
     *
     * <p>作用：
     * - 支持通配符加载资源（classpath*: / file: / http: 等）
     * - 这里用于批量加载 document 目录下所有 md 文件
     */
    private final ResourcePatternResolver resourcePatternResolver;

    public DocumentLoader(ResourcePatternResolver resourcePatternResolver) {
        this.resourcePatternResolver = resourcePatternResolver;
    }

    /**
     * 加载所有 Markdown 文档并转换为 Document 列表
     *
     * @return Document 列表（可直接用于向量化）
     *
     * <p>处理流程：
     * 1. 扫描 classpath:document/*.md
     * 2. 遍历每个 Markdown 文件
     * 3. 解析文件名，提取业务标签（status）
     * 4. 构建 MarkdownReader 配置
     * 5. 解析 Markdown → 多个 Document
     * 6. 合并到总列表
     *
     * <p>注意：
     * - 一个 Markdown 文件 ≠ 一个 Document
     * - 如果开启分段（---），会被拆成多个 Document
     */
    public List<Document> loadMarkdowns() {
        List<Document> allDocuments = new ArrayList<>();

        try {
            // 加载 classpath 下所有 md 文件
            Resource[] resources = resourcePatternResolver.getResources("classpath:document/*.md");

            for (Resource resource : resources) {

                // 获取文件名，例如：编程AI助手_java.md
                String filename = resource.getFilename();

                if (filename == null) {
                    log.warn("检测到空文件名，跳过该资源");
                    continue;
                }

                // ================== 文件名解析 ==================

                // 去掉 .md 后缀
                String nameWithoutExt = filename.replace(".md", "");

                // 按 "_" 分割
                String[] parts = nameWithoutExt.split("_");

                /**
                 * 提取状态标签（技术类型）
                 *
                 * 示例：
                 * 编程AI助手_java → java
                 * 编程AI助手_mysql → mysql
                 *
                 * 如果没有 "_"，则默认 unknown
                 */
                String status = parts.length > 1 ? parts[parts.length - 1] : "unknown";

                // ================== Markdown 解析配置 ==================

                MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()

                        /**
                         * 是否根据 Markdown 中的分隔符（---）拆分文档
                         *
                         * true：
                         *   一个 md → 多个 Document（推荐，利于向量检索粒度更细）
                         *
                         * false：
                         *   一个 md → 一个 Document
                         */
                        .withHorizontalRuleCreateDocument(true)

                        /**
                         * 是否包含代码块
                         *
                         * false：
                         *   忽略 ```code```，减少 embedding 噪声
                         */
                        .withIncludeCodeBlock(true)

                        /**
                         * 是否包含引用块（>）
                         *
                         * false：
                         *   忽略引用内容，减少无效信息
                         */
                        .withIncludeBlockquote(false)

                        /**
                         * 添加 metadata：文件名
                         *
                         * 用途：
                         * - 溯源（回答来自哪个文件）
                         * - UI 展示
                         */
                        .withAdditionalMetadata("filename", filename)

                        /**
                         * 添加 metadata：业务标签（非常关键）
                         *
                         * 用途：
                         * - RAG 检索过滤
                         *   例如：
                         *   WHERE status = 'java'
                         */
                        .withAdditionalMetadata("status", status)

                        .build();

                // ================== Markdown → Document ==================

                MarkdownDocumentReader markdownDocumentReader =
                        new MarkdownDocumentReader(resource, config);

                /**
                 * get() 会返回多个 Document
                 * （如果开启了分段）
                 */
                List<Document> documents = markdownDocumentReader.get();

                log.info("加载文件：{}，解析出 {} 个 Document，status={}",
                        filename, documents.size(), status);

                allDocuments.addAll(documents);
            }

        } catch (IOException e) {
            log.error("Markdown 文档加载失败", e);
        }

        return allDocuments;
    }
}