package com.yl.tools;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * 网页抓取工具类
 *
 * 功能说明：
 * 1. 根据传入的 URL 抓取网页内容
 * 2. 使用 Jsoup 发起 HTTP 请求并解析网页
 * 3. 当前返回的是网页的完整 HTML 源码
 *
 * 适用场景：
 * - 获取网页原始 HTML 内容
 * - 后续可结合 Jsoup 进一步提取标题、正文、链接等信息
 *
 * 注意事项：
 * - 目标网页必须允许正常访问
 * - 某些网站可能存在反爬限制，导致抓取失败
 * - 当前方法返回的是完整 HTML，而不是纯文本正文
 */
public class WebScrapingTool {

    /**
     * 抓取指定网页的内容
     *
     * @param url 要抓取的网页地址
     * @return 抓取成功时返回网页完整 HTML；失败时返回错误信息
     */
    @Tool(description = "Scrape the content of a web page")
    public String scrapeWebPage(
            @ToolParam(description = "URL of the web page to scrape") String url) {

        try {
            // 使用 Jsoup 连接目标网页并获取文档对象
            Document document = Jsoup.connect(url).get();

            // 返回网页的完整 HTML 内容
            return document.html();
        } catch (Exception e) {
            // 捕获抓取异常并返回错误信息
            return "Error scraping web page: " + e.getMessage();
        }
    }
}