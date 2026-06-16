package com.backend.service;

import com.backend.entity.KnowledgeDocument;

public interface KnowledgeEmbeddingService {
    void syncKnowledge(KnowledgeDocument document);
    void deleteByKnowledgeId(String knowledgeId);
    void reindexAll();
    void reindexById(String knowledgeId);
}
