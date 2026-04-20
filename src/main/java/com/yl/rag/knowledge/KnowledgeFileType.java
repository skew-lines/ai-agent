package com.yl.rag.knowledge;

// rag知识库文件类型枚举，包括pdf，markdown，docx，txt
public enum KnowledgeFileType {
    PDF,
    MARKDOWN,
    DOCX,
    TXT;

    public static KnowledgeFileType fromFileName(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".pdf")) {
            return PDF;
        }
        if (lower.endsWith(".md") || lower.endsWith(".markdown")) {
            return MARKDOWN;
        }
        if (lower.endsWith(".docx")) {
            return DOCX;
        }
        if (lower.endsWith(".txt")) {
            return TXT;
        }
        throw new IllegalArgumentException("Unsupported file type: " + fileName);
    }
}

