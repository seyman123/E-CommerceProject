package com.seyman.dreamshops.service.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@Slf4j
public class CacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private boolean redisAvailable = true;

    @Autowired
    public CacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        testRedisConnection();
    }

    private void testRedisConnection() {
        try {
            redisTemplate.opsForValue().set("test", "test", Duration.ofSeconds(1));
            redisTemplate.delete("test");
            log.info("Redis connection successful - Cache enabled");
            redisAvailable = true;
        } catch (Exception e) {
            log.warn("Redis connection failed - Cache disabled, falling back to direct DB access: {}", e.getMessage());
            redisAvailable = false;
        }
    }

    public void put(String key, Object value, Duration ttl) {
        if (!redisAvailable) return;
        
        try {
            redisTemplate.opsForValue().set(key, value, ttl);
        } catch (Exception e) {
            log.warn("Cache put failed for key {}: {}", key, e.getMessage());
            redisAvailable = false;
        }
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key, Class<T> type) {
        if (!redisAvailable) return Optional.empty();
        
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                return Optional.of((T) value);
            }
        } catch (Exception e) {
            log.warn("Cache get failed for key {}: {}", key, e.getMessage());
            redisAvailable = false;
        }
        return Optional.empty();
    }

    public void evict(String key) {
        if (!redisAvailable) return;
        
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("Cache evict failed for key {}: {}", key, e.getMessage());
        }
    }

    public void evictPattern(String pattern) {
        if (!redisAvailable) return;
        
        try {
            redisTemplate.delete(redisTemplate.keys(pattern + "*"));
        } catch (Exception e) {
            log.warn("Cache evict pattern failed for pattern {}: {}", pattern, e.getMessage());
        }
    }

    public boolean isRedisAvailable() {
        return redisAvailable;
    }
} 