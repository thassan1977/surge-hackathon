package com.surge.agent.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebSocketConfig {

    // We are connecting to the Binance ETH/USDT live order book ticker
    @Value("${ai.binance.stream.url}")
    private String BINANCE_WS_URL;

    @Bean
    public WebSocketConnectionManager binanceWsConnectionManager(BinanceWebSocketHandler handler) {
        log.info("Configuring Binance WebSocket Connection to: {}", BINANCE_WS_URL);

        StandardWebSocketClient client = new StandardWebSocketClient();

        WebSocketConnectionManager manager = new WebSocketConnectionManager(
                client,
                handler,
                BINANCE_WS_URL
        );

        // This tells Spring to automatically call start() AFTER the application context is fully initialized
        manager.setAutoStartup(true);

        return manager;
    }
}