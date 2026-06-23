package com.ragdoc.platform.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ragdoc.platform.auth.dto.UserResponse;
import com.ragdoc.platform.user.UserCacheNames;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    @Profile("!test")
    RedisCacheManager redisCacheManager(
            RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper,
            @Value("${app.cache.user-ttl}") Duration userTtl
    ) {
        Jackson2JsonRedisSerializer<UserResponse> serializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, UserResponse.class);

        RedisCacheConfiguration userCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(userTtl)
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));

        return RedisCacheManager.builder(connectionFactory)
                .withCacheConfiguration(UserCacheNames.USER, userCacheConfig)
                .build();
    }

    @Bean
    @Profile("test")
    CacheManager testCacheManager() {
        return new ConcurrentMapCacheManager(UserCacheNames.USER);
    }
}
