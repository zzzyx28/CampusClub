package com.backend.service.impl;

import com.backend.entity.KnowledgeDocument;
import com.backend.service.KnowledgeEmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeEmbeddingServiceImpl implements KnowledgeEmbeddingService {

    private static final String META_KNOWLEDGE_ID = "knowledge_id";
    private static final String META_TITLE = "title";

    private final VectorStore vectorStore;
    private final TokenTextSplitter splitter = new TokenTextSplitter();

    @Override
    public void syncKnowledge(KnowledgeDocument document) {
        if (document == null || !StringUtils.hasText(document.getId())) {
            return;
        }
        deleteByKnowledgeId(document.getId());
        List<Document> chunks = buildChunks(document);
        if (!chunks.isEmpty()) {
            vectorStore.add(chunks);
            log.info("Synced {} embedding chunks for knowledge {}", chunks.size(), document.getId());
        }
    }

    @Override
    public void deleteByKnowledgeId(String knowledgeId) {
        if (!StringUtils.hasText(knowledgeId)) {
            return;
        }
        try {
            List<String> ids = new ArrayList<>();
            for (int i = 0; i < 200; i++) {
                ids.add(knowledgeId + "-" + i);
            }
            vectorStore.delete(ids);
        } catch (Exception e) {
            log.warn("Failed to delete embeddings for knowledge {}: {}", knowledgeId, e.getMessage());
        }
    }

    @Override
    public void reindexAll() {
        // Called from bootstrap with explicit document list
    }

    public void reindexDocuments(List<KnowledgeDocument> documents) {
        for (KnowledgeDocument document : documents) {
            syncKnowledge(document);
        }
    }

    @Override
    public void reindexById(String knowledgeId) {
        // no-op placeholder; KnowledgeServiceImpl loads doc then calls syncKnowledge
    }

    private List<Document> buildChunks(KnowledgeDocument document) {
        String body = document.getTitle() + "\n" + document.getContent();
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(META_KNOWLEDGE_ID, document.getId());
        metadata.put(META_TITLE, document.getTitle());
        if (document.getTags() != null && !document.getTags().isEmpty()) {
            metadata.put("tags", String.join(",", document.getTags()));
        }

        Document source = new Document(document.getId(), body, metadata);
        List<Document> rawChunks = splitter.transform(List.of(source));

        List<Document> chunks = new ArrayList<>();
        for (int i = 0; i < rawChunks.size(); i++) {
            Document chunk = rawChunks.get(i);
            Map<String, Object> chunkMeta = new HashMap<>(metadata);
            chunkMeta.put("chunk_index", i);
            chunks.add(new Document(document.getId() + "-" + i, chunk.getContent(), chunkMeta));
        }
        return chunks;
    }
}
