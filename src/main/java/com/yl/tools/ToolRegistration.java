package com.yl.tools;

import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 工具统一注册配置类
 *
 * 功能说明：
 * 1. 负责集中创建并注册项目中使用到的各类工具对象
 * 2. 通过 Spring 的 @Configuration 和 @Bean，将工具统一交给 Spring 容器管理
 * 3. 最终以 ToolCallback[] 的形式返回，供 Spring AI 统一加载和调用
 *
 * 当前注册的工具包括：
 * - 文件读写工具
 * - Web 搜索工具
 * - 网页抓取工具
 * - 资源下载工具
 * - 终端操作工具
 * - PDF 生成工具
 * - 终止执行工具
 */
@Configuration
public class ToolRegistration {

    /**
     * SearchAPI 的访问密钥
     * 从配置文件中读取：search-api.api-key
     */
    @Value("${search-api.api-key}")
    private String searchApiKey;

    /**
     * Web 搜索接口基础地址
     * 如果配置文件中未提供，则使用默认值：
     * https://www.searchapi.io/api/v1/search
     */
    @Value("${app.web-search.base-url:https://www.searchapi.io/api/v1/search}")
    private String webSearchBaseUrl;

    /**
     * Web 搜索引擎名称
     * 如果未配置，则默认使用 baidu
     */
    @Value("${app.web-search.engine:baidu}")
    private String webSearchEngine;

    /**
     * 文件保存基础目录
     * 如果未配置，则默认保存到：${user.dir}/tmp
     */
    @Value("${app.file.save-dir:${user.dir}/tmp}")
    private String fileSaveDir;

    /**
     * 注册所有工具回调
     *
     * 说明：
     * 1. 先手动实例化每个工具类
     * 2. 再通过 ToolCallbacks.from(...) 转换为 Spring AI 可识别的 ToolCallback 数组
     * 3. 返回后，Spring 容器会将这些工具统一纳入管理
     *
     * @return 所有已注册工具组成的 ToolCallback 数组
     */
    @Bean
    public ToolCallback[] allTools() {
        // 文件操作工具：负责读取/写入本地文件
        FileOperationTool fileOperationTool = new FileOperationTool(fileSaveDir);

        // Web 搜索工具：负责调用搜索接口进行联网搜索
        WebSearchTool webSearchTool = new WebSearchTool(searchApiKey, webSearchBaseUrl, webSearchEngine);

        // 网页抓取工具：负责抓取网页正文内容
        WebScrapingTool webScrapingTool = new WebScrapingTool();

        // 资源下载工具：负责根据 URL 下载资源到本地目录
        ResourceDownloadTool resourceDownloadTool = new ResourceDownloadTool(fileSaveDir);

        // 终端操作工具：负责执行终端命令
        TerminalOperationTool terminalOperationTool = new TerminalOperationTool();

        // PDF 生成工具：负责根据内容生成 PDF 文件
        PDFGenerationTool pdfGenerationTool = new PDFGenerationTool(fileSaveDir);

        // 终止工具：用于中断或停止某些操作
        TerminateTool terminateTool = new TerminateTool();

        // 将所有工具统一转换为 ToolCallback 数组并返回
        return ToolCallbacks.from(
                fileOperationTool,
                webSearchTool,
                webScrapingTool,
                resourceDownloadTool,
                terminalOperationTool,
                pdfGenerationTool,
                terminateTool
        );
    }
}