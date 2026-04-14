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

/**
 * 启动时将静态知识入库，供 QuestionAnswerAdvisor 做向量检索。
 */
@Configuration
public class SmartShopKnowledgeIngestConfig {

    @Bean
    ApplicationRunner ingestSmartShopKnowledge(VectorStore vectorStore,
            @Value("classpath:rag/ecommerce-knowledge.txt") Resource knowledgeResource) {
        return args -> {
            if (!vectorStore.similaritySearch(SearchRequest.defaults().withQuery("SmartShop").withTopK(1)).isEmpty()) {
                return;
            }
            var raw = new TextReader(knowledgeResource).read();
            List<Document> chunks = new TokenTextSplitter().transform(raw);
            vectorStore.add(chunks);
        };
    }
}
