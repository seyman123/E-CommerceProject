package com.seyman.dreamshops.service.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
    private RedisTemplate<String, String> redisTemplate; // String-only Redis template
    
    private final ObjectMapper objectMapper; // Local ObjectMapper for this service only
    
    // Fallback in-memory cache when Redis is not available
    private final ConcurrentMap<String, CacheEntry> inMemoryCache = new ConcurrentHashMap<>();
    
    public CacheService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // Configure Jackson to ignore unknown properties during deserialization
        // This fixes issues with computed fields like effectivePrice and savings
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public void put(String key, Object value, Duration ttl) {
        try {
            if (redisTemplate != null) {
                // Convert object to JSON string for Redis storage
                String jsonValue = objectMapper.writeValueAsString(value);
                redisTemplate.opsForValue().set(key, jsonValue, ttl);
                log.debug("Cached value in Redis (as JSON string) with key: {}", key);
            } else {
                // Fall back to in-memory cache
                long expireTime = System.currentTimeMillis() + ttl.toMillis();
                inMemoryCache.put(key, new CacheEntry(value, expireTime));
                log.debug("Cached value in memory with key: {}", key);
            }
        } catch (Exception e) {
            log.warn("Failed to cache value with key {}: {}", key, e.getMessage());
            // Fallback to in-memory cache if Redis fails
            try {
                long expireTime = System.currentTimeMillis() + ttl.toMillis();
                inMemoryCache.put(key, new CacheEntry(value, expireTime));
                log.debug("Fallback: Cached value in memory with key: {}", key);
            } catch (Exception ex) {
                log.error("Failed to cache in memory as well: {}", ex.getMessage());
            }
        }
    }

    public <T> Optional<T> get(String key, Class<T> type) {
        try {
            if (redisTemplate != null) {
                // Try Redis first - get JSON string and convert back to object
                String jsonValue = redisTemplate.opsForValue().get(key);
                if (jsonValue != null) {
                    T value = objectMapper.readValue(jsonValue, type);
                    log.debug("Cache HIT in Redis for key: {}", key);
                    return Optional.of(value);
                }
            }
            
            // Check in-memory cache
            CacheEntry entry = inMemoryCache.get(key);
            if (entry != null && !entry.isExpired() && type.isInstance(entry.getValue())) {
                log.debug("Cache HIT in memory for key: {}", key);
                return Optional.of(type.cast(entry.getValue()));
            } else if (entry != null && entry.isExpired()) {
                // Remove expired entry
                inMemoryCache.remove(key);
            }
            
            log.debug("Cache MISS for key: {}", key);
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Failed to get cached value for key {}: {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    public <T> Optional<T> get(String key, TypeReference<T> typeReference) {
        try {
            if (redisTemplate != null) {
                // Try Redis first - get JSON string and convert back to object with proper generic type info
                String jsonValue = redisTemplate.opsForValue().get(key);
                if (jsonValue != null) {
                    T value = objectMapper.readValue(jsonValue, typeReference);
                    log.debug("Cache HIT in Redis for key: {} with TypeReference", key);
                    return Optional.of(value);
                }
            }
            
            // For in-memory cache, try to cast if possible
            CacheEntry entry = inMemoryCache.get(key);
            if (entry != null && !entry.isExpired()) {
                try {
                    @SuppressWarnings("unchecked")
                    T value = (T) entry.getValue();
                    log.debug("Cache HIT in memory for key: {} with TypeReference", key);
                    return Optional.of(value);
                } catch (ClassCastException e) {
                    log.debug("Type cast failed for in-memory cache, removing entry: {}", key);
                    inMemoryCache.remove(key);
                }
            } else if (entry != null && entry.isExpired()) {
                // Remove expired entry
                inMemoryCache.remove(key);
            }
            
            log.debug("Cache MISS for key: {} with TypeReference", key);
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Failed to get cached value for key {} with TypeReference: {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    public void delete(String key) {
        try {
            if (redisTemplate != null) {
                redisTemplate.delete(key);
                log.debug("Deleted key from Redis: {}", key);
            }
            inMemoryCache.remove(key);
            log.debug("Deleted key from memory: {}", key);
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
                    log.info("Deleted {} keys matching pattern: {}", keys.size(), pattern);
                }
            }
            // For in-memory cache, remove keys matching pattern
            inMemoryCache.keySet().removeIf(key -> key.matches(pattern.replace("*", ".*")));
            log.debug("Deleted keys matching pattern from memory: {}", pattern);
        } catch (Exception e) {
            log.warn("Failed to delete cached values for pattern {}: {}", pattern, e.getMessage());
        }
    }
    
    public void clearProductCaches() {
        log.info("Clearing all product-related caches due to schema changes...");
        deleteByPattern("products:*");
        log.info("Product caches cleared successfully");
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
