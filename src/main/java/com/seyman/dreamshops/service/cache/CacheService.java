package com.seyman.dreamshops.service.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@Slf4j
public class CacheService {

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;
    
    // Fallback in-memory cache when Redis is not available
    private final ConcurrentMap<String, CacheEntry> inMemoryCache = new ConcurrentHashMap<>();

    public void put(String key, Object value, Duration ttl) {
        try {
            if (redisTemplate != null) {
                // Use Redis if available
                redisTemplate.opsForValue().set(key, value, ttl);
                log.debug("Cached value in Redis with key: {}", key);
            } else {
                // Fall back to in-memory cache
                long expireTime = System.currentTimeMillis() + ttl.toMillis();
                inMemoryCache.put(key, new CacheEntry(value, expireTime));
                log.debug("Cached value in memory with key: {}", key);
            }
        } catch (Exception e) {
            log.warn("Failed to cache value with key {}: {}", key, e.getMessage());
        }
    }

    public <T> Optional<T> get(String key, Class<T> type) {
        try {
            if (redisTemplate != null) {
                // Try Redis first
                Object value = redisTemplate.opsForValue().get(key);
                if (value != null && type.isInstance(value)) {
                    log.debug("Cache HIT in Redis for key: {}", key);
                    return Optional.of(type.cast(value));
                }
            } else {
                // Check in-memory cache
                CacheEntry entry = inMemoryCache.get(key);
                if (entry != null && !entry.isExpired() && type.isInstance(entry.getValue())) {
                    log.debug("Cache HIT in memory for key: {}", key);
                    return Optional.of(type.cast(entry.getValue()));
                } else if (entry != null && entry.isExpired()) {
                    // Remove expired entry
                    inMemoryCache.remove(key);
                }
            }
            
            log.debug("Cache MISS for key: {}", key);
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Failed to get cached value for key {}: {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    public void delete(String key) {
        try {
            if (redisTemplate != null) {
                redisTemplate.delete(key);
                log.debug("Deleted key from Redis: {}", key);
            } else {
                inMemoryCache.remove(key);
                log.debug("Deleted key from memory: {}", key);
            }
        } catch (Exception e) {
            log.warn("Failed to delete cached value for key {}: {}", key, e.getMessage());
        }
    }

    public void deleteByPattern(String pattern) {
        try {
            if (redisTemplate != null) {
                var keys = redisTemplate.keys(pattern);
                if (keys != null && !keys.isEmpty()) {
                    redisTemplate.delete(keys);
                    log.debug("Deleted {} keys matching pattern: {}", keys.size(), pattern);
                }
            } else {
                // For in-memory cache, remove keys matching pattern
                inMemoryCache.keySet().removeIf(key -> key.matches(pattern.replace("*", ".*")));
                log.debug("Deleted keys matching pattern from memory: {}", pattern);
            }
        } catch (Exception e) {
            log.warn("Failed to delete cached values for pattern {}: {}", pattern, e.getMessage());

        }
    }

    // Inner class for in-memory cache entries
    private static class CacheEntry {
        private final Object value;
        private final long expireTime;

        public CacheEntry(Object value, long expireTime) {
            this.value = value;
            this.expireTime = expireTime;
        }

        public Object getValue() {
            return value;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expireTime;
        }
    }
}
