package com.yl.tools;

import cn.hutool.core.io.FileUtil;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.IOException;

/**
 * PDF 生成工具类
 *
 * 功能说明：
 * 1. 根据传入的文件名和文本内容生成 PDF 文件
 * 2. 自动在 baseDir/pdf 目录下创建目标文件
 * 3. 使用 iText 进行 PDF 文档创建
 * 4. 默认使用内置中文字体，避免中文乱码
 */
public class PDFGenerationTool {

    /**
     * 基础目录
     * 所有生成的 PDF 文件都会保存在该目录下的 pdf 子目录中
     */
    private final String baseDir;

    /**
     * 构造方法
     *
     * @param baseDir 基础路径，例如项目运行目录
     */
    public PDFGenerationTool(String baseDir) {
        this.baseDir = baseDir;
    }

    /**
     * 生成 PDF 文件
     *
     * @param fileName 生成后的 PDF 文件名，例如 test.pdf
     * @param content  要写入 PDF 的文本内容
     * @return 生成结果信息
     */
    @Tool(description = "Generate a PDF file with given content", returnDirect = false)
    public String generatePDF(
            @ToolParam(description = "Name of the file to save the generated PDF") String fileName,
            @ToolParam(description = "Content to be included in the PDF") String content) {

        // PDF 文件存储目录
        String fileDir = baseDir + "/pdf";

        // PDF 文件完整路径
        String filePath = fileDir + "/" + fileName;

        try {
            // 创建目录，如果目录不存在则自动创建
            FileUtil.mkdir(fileDir);

            // 创建 PdfWriter、PdfDocument 和 Document 对象
            // 这里使用 try-with-resources，确保资源自动关闭
            try (PdfWriter writer = new PdfWriter(filePath);
                 PdfDocument pdf = new PdfDocument(writer);
                 Document document = new Document(pdf)) {

                /*
                 * 方式一：使用自定义字体文件
                 * 适用于需要严格控制字体样式的场景
                 *
                 * 示例：
                 * String fontPath = Paths.get("src/main/resources/static/fonts/simsun.ttf")
                 *         .toAbsolutePath().toString();
                 * PdfFont font = PdfFontFactory.createFont(
                 *         fontPath,
                 *         PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED
                 * );
                 */

                // 方式二：使用 iText 内置中文字体，避免中文显示乱码
                PdfFont font = PdfFontFactory.createFont("STSongStd-Light", "UniGB-UCS2-H");
                document.setFont(font);

                // 创建段落对象，并将内容写入段落
                Paragraph paragraph = new Paragraph(content);

                // 将段落添加到文档中
                document.add(paragraph);
            }

            return "PDF generated successfully to: " + filePath;
        } catch (IOException e) {
            return "Error generating PDF: " + e.getMessage();
        }
    }
}