package com.yl.rag.chunk;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MarkdownSemanticChunkService {

    private final TokenTextSplitter tokenTextSplitter;

    public MarkdownSemanticChunkService() {
        this.tokenTextSplitter = TokenTextSplitter.builder()
                .withChunkSize(400)
                .withMinChunkSizeChars(200)
                .withMinChunkLengthToEmbed(10)
                .withMaxNumChunks(2000)
                .withKeepSeparator(true)
                .build();
    }

    public List<Document> chunk(List<Document> readerDocuments, String documentId, String fileName, String kbId) {
        List<Document> chunkDocuments = new ArrayList<>();

        String currentSectionTitle = null;
        String currentHeaderCategory = null;
        int chunkIndex = 0;

        for (Document doc : readerDocuments) {
            String text = doc.getText();
            if (text == null || text.isBlank()) {
                continue;
            }

            Map<String, Object> sourceMetadata = new HashMap<>(doc.getMetadata());
            String category = getStringMetadata(sourceMetadata, "category");

            if (isHeader(category)) {
                currentSectionTitle = getStringMetadata(sourceMetadata, "title");
                currentHeaderCategory = category;
                continue;
            }

            if (isCodeBlock(category)) {
                Map<String, Object> chunkMetadata = buildChunkMetadata(
                        sourceMetadata, documentId, fileName, kbId, chunkIndex++, currentSectionTitle, currentHeaderCategory
                );
                chunkMetadata.put("chunkType", "code");

                chunkDocuments.add(Document.builder()
                        .text(text.trim())
                        .metadata(chunkMetadata)
                        .build());
                continue;
            }

            List<String> textChunks = tokenTextSplitter.apply(List.of(doc))
                    .stream()
                    .map(Document::getText)
                    .toList();

            for (String part : textChunks) {
                if (part == null || part.isBlank()) {
                    continue;
                }

                Map<String, Object> chunkMetadata = buildChunkMetadata(
                        sourceMetadata, documentId, fileName, kbId, chunkIndex++, currentSectionTitle, currentHeaderCategory
                );
                chunkMetadata.put("chunkType", "text");

                chunkDocuments.add(Document.builder()
                        .text(part.trim())
                        .metadata(chunkMetadata)
                        .build());
            }
        }

        return chunkDocuments;
    }

    private Map<String, Object> buildChunkMetadata(Map<String, Object> sourceMetadata,
                                                   String documentId,
                                                   String fileName,
                                                   String kbId,
                                                   int chunkIndex,
                                                   String currentSectionTitle,
                                                   String currentHeaderCategory) {
        Map<String, Object> metadata = new HashMap<>(sourceMetadata);
        metadata.put("documentId", documentId);
        metadata.put("fileName", fileName);
        metadata.put("kbId", kbId);
        metadata.put("chunkIndex", chunkIndex);
        metadata.put("sectionTitle", currentSectionTitle);
        metadata.put("sectionCategory", currentHeaderCategory);
        return metadata;
    }

    private boolean isHeader(String category) {
        return category != null && category.startsWith("header_");
    }

    private boolean isCodeBlock(String category) {
        return "code_block".equals(category);
    }

    private String getStringMetadata(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        return value == null ? null : String.valueOf(value);
    }
}

