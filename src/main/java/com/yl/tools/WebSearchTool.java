package com.yl.tools;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Web 搜索工具类
 *
 * 功能说明：
 * 1. 调用 SearchAPI 的搜索接口进行联网搜索
 * 2. 根据传入的查询关键字，从指定搜索引擎中获取搜索结果
 * 3. 当前默认返回前 5 条自然搜索结果（organic_results）
 *
 * 实现说明：
 * - 使用 Hutool 的 HttpUtil 发起 GET 请求
 * - 使用 Hutool 的 JSON 工具解析返回结果
 * - 支持通过构造方法注入 API Key、搜索接口地址和搜索引擎名称
 *
 * 注意事项：
 * - 当前方法返回的是 JSON 字符串拼接结果，不是格式化后的文本
 * - 如果接口返回异常或无结果，会返回错误原因或原始响应内容
 */
public class WebSearchTool {

    /**
     * SearchAPI 的访问密钥
     */
    private final String apiKey;

    /**
     * 搜索接口地址
     */
    private final String searchApiUrl;

    /**
     * 搜索引擎名称
     * 例如：baidu、google 等
     */
    private final String engine;

    /**
     * 构造方法
     *
     * @param apiKey       SearchAPI 的 API Key
     * @param searchApiUrl 搜索接口地址
     * @param engine       搜索引擎名称
     */
    public WebSearchTool(String apiKey, String searchApiUrl, String engine) {
        this.apiKey = apiKey;
        this.searchApiUrl = searchApiUrl;
        this.engine = engine;
    }

    /**
     * 执行 Web 搜索
     *
     * @param query 搜索关键词
     * @return 搜索结果字符串；如果失败则返回错误信息
     */
    @Tool(description = "Search for information from Baidu Search Engine")
    public String searchWeb(@ToolParam(description = "Search query keyword") String query) {
        // 请求参数
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("q", query);
        paramMap.put("api_key", apiKey);
        paramMap.put("engine", engine);

        try {
            // 发起 GET 请求，调用搜索接口
            String response = HttpUtil.get(searchApiUrl, paramMap);

            // 将响应字符串解析为 JSON 对象
            JSONObject jsonObject = JSONUtil.parseObj(response);

            // 获取自然搜索结果列表
            JSONArray organicResults = jsonObject.getJSONArray("organic_results");

            // 如果没有自然搜索结果，则尝试提取错误信息
            if (organicResults == null || organicResults.isEmpty()) {
                Object errorMessage = jsonObject.getByPath("error.message");
                if (errorMessage == null) {
                    errorMessage = jsonObject.getStr("error");
                }
                if (errorMessage == null) {
                    errorMessage = jsonObject.getStr("message");
                }

                return "No organic search results returned by SearchAPI. "
                        + (errorMessage == null ? "Raw response: " + response : "Reason: " + errorMessage);
            }

            // 最多返回前 5 条搜索结果
            int resultSize = Math.min(5, organicResults.size());
            List<Object> objects = organicResults.subList(0, resultSize);

            // 将结果列表中的每一项 JSON 对象转为字符串，并用逗号拼接返回
            return objects.stream()
                    .map(obj -> ((JSONObject) obj).toString())
                    .collect(Collectors.joining(","));

        } catch (Exception e) {
            // 捕获异常并返回错误信息
            return "Error searching Baidu: " + e.getMessage();
        }
    }
}