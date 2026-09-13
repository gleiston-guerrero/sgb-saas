package com.uteq.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.interceptor.CacheErrorHandler;

import java.time.Duration;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisConfig {

    // Caches con serialización JDK estándar (PageImpl no deserializa en JSON).
    // "libros": paginado del catálogo. "sugerencias-libros": autocompletado con TTL corto.
    /**
     * Procesa cache manager y devuelve el resultado calculado por el backend.
     *
     * @param connectionFactory valor de entrada connectionFactory usado por la operacion para completar su regla de negocio
     * @param booksTtlSeconds valor de entrada booksTtlSeconds usado por la operacion para completar su regla de negocio
     * @param suggestionsTtlSeconds valor de entrada suggestionsTtlSeconds usado por la operacion para completar su regla de negocio
     * @return objeto con el resultado de la operacion y los datos relevantes para el cliente
     */
    @Bean
    public CacheManager cacheManager(
            RedisConnectionFactory connectionFactory,
            @Value("${app.cache.libros.ttl-seconds}") long booksTtlSeconds,
            @Value("${app.cache.sugerencias.ttl-seconds}") long suggestionsTtlSeconds) {
        RedisCacheConfiguration baseConfig = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues();

        // TTL externo (application.yml), nunca hardcodeado en Java.
        RedisCacheConfiguration booksConfig = baseConfig.entryTtl(Duration.ofSeconds(booksTtlSeconds));

        // TTL propio en segundos: el autocompletado se teclea letra por letra.
        RedisCacheConfiguration suggestionsConfig = baseConfig.entryTtl(Duration.ofSeconds(suggestionsTtlSeconds));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(baseConfig)
                .withInitialCacheConfigurations(Map.of(
                        "libros", booksConfig,
                        "sugerencias-libros", suggestionsConfig))
                .transactionAware()
                .build();
    }

    @Bean
    /**
     * Handles cache error handler.
     *
     * @return cache error handler with the resulting state after the operation
     */
    public CacheErrorHandler cacheErrorHandler() {
        return new CacheErrorHandler() {
            private final Logger log = LoggerFactory.getLogger(CacheErrorHandler.class);
            /**
             * Logs a cache read failure and lets the request continue without
             * cache (Redis works as a degraded optional layer, never fatal).
             *
             * @param e runtime failure raised by the cache read
             * @param cache cache region where the read failed
             * @param key key that could not be read from the cache
             */
            @Override public void handleCacheGetError(RuntimeException e, org.springframework.cache.Cache cache, Object key) {
                log.warn("Cache get error (Redis degradado) cache={} key={}: {}", cache.getName(), key, e.toString());
            }
            /**
             * Logs a cache write failure and lets the request continue; the
             * response is still served even though it was not cached.
             *
             * @param e runtime failure raised by the cache write
             * @param cache cache region where the write failed
             * @param key key that could not be written to the cache
             * @param value value that could not be stored in the cache
             */
            @Override public void handleCachePutError(RuntimeException e, org.springframework.cache.Cache cache, Object key, Object value) {
                log.warn("Cache put error (Redis degradado) cache={} key={}: {}", cache.getName(), key, e.toString());
            }
            /**
             * Logs a cache eviction failure; a stale entry may survive until
             * its TTL expires, which the short TTLs keep harmless.
             *
             * @param e runtime failure raised by the cache eviction
             * @param cache cache region where the eviction failed
             * @param key key that could not be evicted from the cache
             */
            @Override public void handleCacheEvictError(RuntimeException e, org.springframework.cache.Cache cache, Object key) {
                log.warn("Cache evict error (Redis degradado) cache={} key={}: {}", cache.getName(), key, e.toString());
            }
            /**
             * Logs a cache clear failure; entries remain until TTL expiry
             * instead of being wiped at once.
             *
             * @param e runtime failure raised by the cache clear
             * @param cache cache region that could not be cleared
             */
            @Override public void handleCacheClearError(RuntimeException e, org.springframework.cache.Cache cache) {
                log.warn("Cache clear error (Redis degradado) cache={}: {}", cache.getName(), e.toString());
            }
        };
    }

    @Bean
    /**
     * Procesa redis template y devuelve el resultado calculado por el backend.
     *
     * @param connectionFactory valor de entrada connectionFactory usado por la operacion para completar su regla de negocio
     * @return objeto con el resultado de la operacion y los datos relevantes para el cliente
     */
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}
