package com.cetide.im.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

@Configuration
public class WebSocketConfig {
    /**
     * 开启WebSocket支持, 自动注册所有使用@ServerEndpoint注解声明的Websocket endpoint
     * @return
     */
    @Bean
    public ServerEndpointExporter webSocketServer() {
        return new ServerEndpointExporter();
    }
}
