package com.surge.agent.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.surge.agent.dto.TradeRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenPositionStore {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String KEY_PREFIX = "open_trades:";
    private static final long TTL_SECONDS = 86400; // 24 hours

    public void save(String tradeId, TradeRecord record) {
        try {
            String key = KEY_PREFIX + tradeId;
            String json = objectMapper.writeValueAsString(record);
            redisTemplate.opsForValue().set(key, json, TTL_SECONDS, TimeUnit.SECONDS);
            log.debug("Saved open trade {} to Redis", tradeId);
        } catch (Exception e) {
            log.error("Failed to save open trade {}: {}", tradeId, e.getMessage());
        }
    }

    public TradeRecord load(String tradeId) {
        try {
            String key = KEY_PREFIX + tradeId;
            String json = redisTemplate.opsForValue().get(key);
            if (json != null) {
                return objectMapper.readValue(json, TradeRecord.class);
            }
        } catch (Exception e) {
            log.error("Failed to load open trade {}: {}", tradeId, e.getMessage());
        }
        return null;
    }

    public List<TradeRecord> loadAll() {
        // Redis does not support scanning by pattern in a simple way; we can use keys()
        // For production, use SCAN; for hackathon, keys() is fine.
        List<TradeRecord> records = new ArrayList<>();
        try {
            Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
            if (keys != null) {
                for (String key : keys) {
                    String json = redisTemplate.opsForValue().get(key);
                    if (json != null) {
                        records.add(objectMapper.readValue(json, TradeRecord.class));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to load all open trades: {}", e.getMessage());
        }
        return records;
    }

    public void delete(String tradeId) {
        try {
            String key = KEY_PREFIX + tradeId;
            redisTemplate.delete(key);
            log.debug("Deleted open trade {} from Redis", tradeId);
        } catch (Exception e) {
            log.error("Failed to delete open trade {}: {}", tradeId, e.getMessage());
        }
    }
}