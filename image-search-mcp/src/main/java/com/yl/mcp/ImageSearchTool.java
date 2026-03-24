package com.yl.mcp;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ImageSearchTool {

    @Value("${image-search.api-key}")
    private String apiKey;

    @Value("${image-search.api-url}")
    private String apiUrl;

    @Tool(description = "search image from web")
    public String searchImage(@ToolParam(description = "Search query keyword") String query) {
        try {
            return String.join(",", searchMediumImages(query));
        } catch (Exception e) {
            return "Error search image: " + e.getMessage();
        }
    }

    /**
     * 搜索中等尺寸的图片列表
     *
     * @param query 搜索关键词
     * @return 图片 URL 列表
     */
    public List<String> searchMediumImages(String query) {
        // 设置请求头（包含 API 密钥）
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", apiKey);

        // 设置请求参数（仅包含 query，可根据文档补充分页等参数）
        Map<String, Object> params = new HashMap<>();
        params.put("query", query);

        // 发送 GET 请求
        String response = HttpUtil.createGet(apiUrl)
                .addHeaders(headers)
                .form(params)
                .execute()
                .body();

        // 解析响应 JSON：
        // 假设响应结构中包含 photos 数组，
        // 每个元素中包含 src 对象，src 中包含 medium 字段
        return JSONUtil.parseObj(response)
                .getJSONArray("photos")
                .stream()
                .map(photoObj -> (JSONObject) photoObj)
                .map(photoObj -> photoObj.getJSONObject("src"))
                .map(photo -> photo.getStr("medium"))
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toList());
    }
}