package com.backend.config;

import com.backend.entity.KnowledgeDocument;
import com.backend.service.KnowledgeService;
import com.backend.service.impl.KnowledgeEmbeddingServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class CampusClubKnowledgeIngestConfig {

    private final KnowledgeService knowledgeService;
    private final KnowledgeEmbeddingServiceImpl knowledgeEmbeddingService;

    @Bean
    @ConditionalOnProperty(name = "app.knowledge.ingest.enabled", havingValue = "true", matchIfMissing = true)
    ApplicationRunner ingestCampusClubKnowledge(
            @Value("classpath:rag/campus-club-knowledge.txt") Resource knowledgeResource) {
        return args -> {
            List<KnowledgeDocument> existing = knowledgeService.listAll();
            if (existing.isEmpty()) {
                String content = new TextReader(knowledgeResource).read()
                        .stream()
                        .map(Document::getContent)
                        .reduce((a, b) -> a + "\n" + b)
                        .orElse("");
                knowledgeService.addKnowledge("CampusClub 默认知识库", content, List.of("默认", "政策", "FAQ"));
            } else {
                knowledgeEmbeddingService.reindexDocuments(existing);
            }
        };
    }
}
