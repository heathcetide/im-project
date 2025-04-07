package com.codeforge.im.config;

import com.codeforge.im.model.ChatMessage;
import com.codeforge.im.model.MessageType;
import com.codeforge.im.service.OnlineUserService;
import com.codeforge.im.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketEventListener {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);
    private final SimpMessageSendingOperations messagingTemplate;
    private final OnlineUserService onlineUserService;
    private final UserService userService;

    public WebSocketEventListener(SimpMessageSendingOperations messagingTemplate, 
                                 OnlineUserService onlineUserService,
                                 UserService userService) {
        this.messagingTemplate = messagingTemplate;
        this.onlineUserService = onlineUserService;
        this.userService = userService;
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        logger.info("Received a new web socket connection");
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String username = (String) headerAccessor.getSessionAttributes().get("username");
        String sessionId = headerAccessor.getSessionId();
        
        if (username != null) {
            logger.info("User Disconnected: {}", username);
            
            // 从在线用户列表中移除
            onlineUserService.removeUser(username, sessionId);
            
            // 更新用户在线状态
            userService.findByUsername(username).ifPresent(user -> {
                user.setOnline(false);
                userService.saveUser(user);
            });
            
            // 发送用户离开消息
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setType(MessageType.LEAVE);
            chatMessage.setSender(username);
            
            messagingTemplate.convertAndSend("/topic/public", chatMessage);
            
            // 通知所有用户更新在线用户列表
            messagingTemplate.convertAndSend("/topic/users", onlineUserService.getOnlineUsers());
        }
    }
} 