package com.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.RedisVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import io.micrometer.observation.ObservationRegistry;
import redis.clients.jedis.JedisPooled;

@Configuration
@Profile("!in-memory-vectors")
public class RedisVectorsConfiguration {
    @Bean(destroyMethod = "close")
    JedisPooled vectorRedis(@Value("${spring.data.redis.host}") String host,
                           @Value("${spring.data.redis.port:6379}") int port) {
        return new JedisPooled(host, port);
    }

    @Bean
    VectorStore redisVectors(JedisPooled redis, EmbeddingModel embeddings, ObservationRegistry observation,
                             @Value("${spring.ai.vectorstore.redis.index-name}") String index,
                             @Value("${spring.ai.vectorstore.redis.prefix:campus:vector:}") String prefix) {
        var config = RedisVectorStore.RedisVectorStoreConfig.builder()
                .withIndexName(index).withPrefix(prefix).build();
        return new RedisVectorStore(config, embeddings, redis, true, observation, null);
    }
}
