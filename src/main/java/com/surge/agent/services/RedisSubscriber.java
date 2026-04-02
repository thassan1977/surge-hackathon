package com.surge.agent.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.surge.agent.model.TradeSignal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;
import java.io.IOException;

    @Slf4j
    @Service
    @RequiredArgsConstructor
    public class RedisSubscriber implements MessageListener {

        private final ObjectMapper objectMapper;
        private final TradeService tradeService;

        @Override
        public void onMessage(Message message, byte[] pattern) {
            try {
                String body = new String(message.getBody());
                log.debug("Received Redis message: {}", body);
                TradeSignal signal = objectMapper.readValue(body, TradeSignal.class);
                tradeService.processSignal(signal);
            } catch (IOException e) {
                log.error("Failed to parse trade signal", e);
            }
        }
    }