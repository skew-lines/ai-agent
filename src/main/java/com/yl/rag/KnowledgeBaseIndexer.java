package com.yl.rag;

import com.yl.AiAgentApplication;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * 知识库向量索引构建入口（离线任务）
 *
 * <p>作用：
 * 用于将原始文档数据构建为向量索引，并写入 pgvector 数据库，
 * 通常在系统初始化或数据更新时手动触发执行。</p>
 *
 * <p>典型流程：</p>
 * <pre>
 * 文档加载 → 文本切分 → 关键词增强 → 向量化（Embedding） → 存入 pgvector
 * </pre>
 *
 * <p>特点：</p>
 * <ul>
 *     <li>以“非 Web 应用”的方式启动（不启动 Tomcat/Netty）</li>
 *     <li>一次性执行，执行完自动退出</li>
 *     <li>适合离线构建索引（Indexing Job）</li>
 * </ul>
 *
 * <p>使用场景：</p>
 * <ul>
 *     <li>首次构建知识库向量索引</li>
 *     <li>知识库数据更新后重新索引</li>
 *     <li>定时任务或手动触发的数据重建</li>
 * </ul>
 *
 * <p>注意：</p>
 * <ul>
 *     <li>该过程通常较耗时（涉及 LLM 调用 + 向量计算）</li>
 *     <li>建议在离线环境执行，避免影响在线服务性能</li>
 * </ul>
 */
@Slf4j
public class KnowledgeBaseIndexer {

    public static void main(String[] args) {

        // 使用 try-with-resources，确保 Spring 容器在任务执行完后自动关闭
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(AiAgentApplication.class)

                // 设置为非 Web 应用（不会启动 Web 服务器，如 Tomcat）
                .web(WebApplicationType.NONE)

                // 关闭启动日志（减少控制台输出）
                .logStartupInfo(false)

                // 启动 Spring 容器
                .run(args)) {

            // 从容器中获取知识库索引服务
            KnowledgeBaseIndexService knowledgeBaseIndexService =
                    context.getBean(KnowledgeBaseIndexService.class);

            /**
             * 执行向量库重建
             *
             * @param false 表示不执行全量删除（具体逻辑取决于实现）
             *              一般可能代表：
             *              - false：增量更新 or 覆盖写入
             *              - true：先清空再重建（全量重建）
             */
            int indexedCount = knowledgeBaseIndexService.rebuildPgVectorStore(false);

            // 输出索引完成日志
            log.info("Knowledge base indexing completed, indexedCount={}", indexedCount);
        }
    }
}