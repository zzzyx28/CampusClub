package com.backend.service.impl;

import com.backend.entity.KnowledgeDocument;
import com.backend.dao.mongo.KnowledgeDocumentRepository;
import com.backend.service.KnowledgeEmbeddingService;
import com.backend.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KnowledgeServiceImpl implements KnowledgeService {

    private final KnowledgeDocumentRepository knowledgeDao;
    private final KnowledgeEmbeddingService knowledgeEmbeddingService;

    @Override
    public KnowledgeDocument addKnowledge(String title, String content, List<String> tags) {
        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setTitle(title);
        doc.setContent(content);
        doc.setTags(tags);
        doc.setLastUpdated(new Date());
        KnowledgeDocument saved = knowledgeDao.save(doc);
        knowledgeEmbeddingService.syncKnowledge(saved);
        return saved;
    }

    @Override
    public List<KnowledgeDocument> searchByTitle(String keyword) {
        return knowledgeDao.findByTitleContaining(keyword);
    }

    @Override
    public void updateContentById(String id, KnowledgeDocument newDoc) {
        KnowledgeDocument doc = knowledgeDao.findById(id)
            .orElseThrow(() -> new RuntimeException("文档不存在"));
        doc.setTitle(newDoc.getTitle());
        doc.setContent(newDoc.getContent());
        doc.setTags(newDoc.getTags());
        doc.setLastUpdated(new Date());
        KnowledgeDocument saved = knowledgeDao.save(doc);
        knowledgeEmbeddingService.syncKnowledge(saved);
    }

    @Override
    public void deleteById(String id) {
        knowledgeEmbeddingService.deleteByKnowledgeId(id);
        knowledgeDao.deleteById(id);
    }

    @Override
    public List<KnowledgeDocument> listAll() {
        return knowledgeDao.findAll();
    }

    @Override
    public void reindexAll() {
        List<KnowledgeDocument> documents = knowledgeDao.findAll();
        for (KnowledgeDocument document : documents) {
            knowledgeEmbeddingService.syncKnowledge(document);
        }
    }

    @Override
    public void reindexById(String id) {
        knowledgeDao.findById(id).ifPresent(knowledgeEmbeddingService::syncKnowledge);
    }
}
