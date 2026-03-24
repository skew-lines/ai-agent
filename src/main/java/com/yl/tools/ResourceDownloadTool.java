package com.yl.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.File;

/**
 * 资源下载工具类
 *
 * 功能说明：
 * 1. 根据指定的 URL 下载网络资源
 * 2. 自动将下载的文件保存到 baseDir/download 目录下
 * 3. 如果目标目录不存在，会自动创建
 * 4. 下载失败时会捕获异常并返回错误信息
 */
public class ResourceDownloadTool {

    /**
     * 基础目录
     * 所有下载的资源都会保存在该目录下的 download 子目录中
     */
    private final String baseDir;

    /**
     * 构造方法
     *
     * @param baseDir 基础目录，例如项目运行路径
     */
    public ResourceDownloadTool(String baseDir) {
        this.baseDir = baseDir;
    }

    /**
     * 下载指定 URL 的资源文件
     *
     * @param url      要下载的资源地址
     * @param fileName 保存后的文件名
     * @return 下载结果信息
     */
    @Tool(description = "Download a resource from a given URL")
    public String downloadResource(
            @ToolParam(description = "URL of the resource to download") String url,
            @ToolParam(description = "Name of the file to save the downloaded resource") String fileName) {

        // 下载目录：baseDir/download
        String fileDir = baseDir + "/download";

        // 下载后的完整文件路径
        String filePath = fileDir + "/" + fileName;

        try {
            // 创建目录，如果目录不存在则自动创建
            FileUtil.mkdir(fileDir);

            // 使用 Hutool 提供的 downloadFile 方法下载资源到指定文件
            HttpUtil.downloadFile(url, new File(filePath));

            return "Resource downloaded successfully to: " + filePath;
        } catch (Exception e) {
            // 捕获下载过程中的异常并返回错误信息
            return "Error downloading resource: " + e.getMessage();
        }
    }
}