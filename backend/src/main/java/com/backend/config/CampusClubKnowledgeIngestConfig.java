package com.backend.config;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.boot.ApplicationRunner;

import java.util.List;

@Configuration
public class CampusClubKnowledgeIngestConfig {

    @Bean
    ApplicationRunner ingestCampusClubKnowledge(VectorStore vectorStore,
            @Value("classpath:rag/campus-club-knowledge.txt") Resource knowledgeResource) {
        return args -> {
            if (!vectorStore.similaritySearch(SearchRequest.defaults().withQuery("CampusClub").withTopK(1)).isEmpty()) {
                return;
            }
            var raw = new TextReader(knowledgeResource).read();
            List<Document> chunks = new TokenTextSplitter().transform(raw);
            vectorStore.add(chunks);
        };
    }
}
