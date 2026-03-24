package com.yl.tools;

import cn.hutool.core.io.FileUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * 文件操作工具类（提供文件读取和写入功能）
 *
 * 说明：
 * 1. 基于 Hutool 的 FileUtil 实现文件读写
 * 2. 所有文件操作都在指定的 baseDir/file 目录下进行
 * 3. 已对异常进行捕获，避免程序中断
 */
public class FileOperationTool {

    /**
     * 文件存储目录（baseDir + "/file"）
     */
    private final String fileDir;

    /**
     * 构造方法
     *
     * @param baseDir 基础目录（例如：项目根路径）
     */
    public FileOperationTool(String baseDir) {
        this.fileDir = baseDir + "/file";
    }

    /**
     * 读取文件内容
     *
     * @param fileName 文件名（不包含路径）
     * @return 文件内容（UTF-8编码），如果失败返回错误信息
     */
    @Tool(description = "Read content from a file")
    public String readFile(
            @ToolParam(description = "Name of a file to read") String fileName) {

        // 拼接完整文件路径
        String filePath = fileDir + "/" + fileName;

        try {
            // 使用 Hutool 读取 UTF-8 文件内容
            return FileUtil.readUtf8String(filePath);
        } catch (Exception e) {
            // 捕获异常并返回错误信息
            return "Error reading file: " + e.getMessage();
        }
    }

    /**
     * 写入内容到文件
     *
     * @param fileName 文件名
     * @param content  要写入的内容
     * @return 操作结果信息
     */
    @Tool(description = "Write content to a file")
    public String writeFile(
            @ToolParam(description = "Name of the file to write") String fileName,
            @ToolParam(description = "Content to write to the file") String content) {

        // 拼接完整文件路径
        String filePath = fileDir + "/" + fileName;

        try {
            // 创建目录（如果不存在）
            FileUtil.mkdir(fileDir);

            // 写入文件（UTF-8编码）
            FileUtil.writeUtf8String(content, filePath);

            return "File written successfully to: " + filePath;
        } catch (Exception e) {
            // 捕获异常并返回错误信息
            return "Error writing to file: " + e.getMessage();
        }
    }
}