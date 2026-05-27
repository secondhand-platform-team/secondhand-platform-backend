package com.secondhand.coreservice.config;

import java.time.Duration;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Use custom ObjectMapper with JavaTimeModule for LocalDateTime support.
        // activateDefaultTyping is NOT needed since we only cache simple category data now.
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer));

        // Per-cache TTL — mỗi cache name có TTL riêng phù hợp với tần suất thay đổi
        java.util.Map<String, RedisCacheConfiguration> cacheConfigurations = new java.util.HashMap<>();
        cacheConfigurations.put("itemDetail", defaults.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("itemsFeatured", defaults.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put("categoriesAll", defaults.entryTtl(Duration.ofMinutes(30)));
        cacheConfigurations.put("categoriesTopLevel", defaults.entryTtl(Duration.ofMinutes(30)));
        cacheConfigurations.put("categoriesChildren", defaults.entryTtl(Duration.ofMinutes(30)));
        cacheConfigurations.put("categoriesChildrenBySlug", defaults.entryTtl(Duration.ofMinutes(30)));
        cacheConfigurations.put("categoryAttributes", defaults.entryTtl(Duration.ofMinutes(30)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}
