package com.yl.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * 终端操作工具类
 *
 * 功能说明：
 * 1. 用于在本机终端中执行指定命令
 * 2. 当前实现基于 Windows 的 cmd.exe
 * 3. 返回命令执行后的标准输出内容
 *
 * 注意事项：
 * 1. 当前实现仅适用于 Windows 环境
 * 2. 如果命令执行失败，会返回错误码或异常信息
 * 3. 该工具存在较高安全风险，应谨慎开放给外部调用
 */
public class TerminalOperationTool {

    /**
     * 执行终端命令
     *
     * @param command 要在终端中执行的命令
     * @return 命令执行结果（标准输出内容或错误信息）
     */
    @Tool(description = "Execute a command in the terminal")
    public String executeTerminalCommand(
            @ToolParam(description = "Command to execute in the terminal") String command) {

        // 用于拼接命令执行后的输出结果
        StringBuilder output = new StringBuilder();

        try {
            /*
             * 创建进程构建器
             *
             * 参数说明：
             * - cmd.exe：Windows 命令解释器
             * - /c：执行指定命令后关闭命令窗口
             * - command：需要执行的具体命令
             */
            ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", command);

            // 另一种方式是直接使用 Runtime.exec(command)，但这里推荐 ProcessBuilder，更灵活
//            Process process = Runtime.getRuntime().exec(command);

            // 启动进程
            Process process = builder.start();

            // 读取命令执行后的标准输出
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            // 等待命令执行完成，并获取退出码
            int exitCode = process.waitFor();

            // 如果退出码不为 0，说明命令执行失败
            if (exitCode != 0) {
                output.append("Command execution failed with exit code: ").append(exitCode);
            }

        } catch (IOException | InterruptedException e) {
            // 捕获命令执行过程中的异常
            output.append("Error executing command: ").append(e.getMessage());
        }

        return output.toString();
    }
}