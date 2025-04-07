package com.codeforge.im.service;

import com.codeforge.im.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OnlineUserService {
    private static final Logger logger = LoggerFactory.getLogger(OnlineUserService.class);
    
    private final Map<String, String> userSessionMap = new ConcurrentHashMap<>();
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;

    public OnlineUserService(UserService userService, SimpMessagingTemplate messagingTemplate) {
        this.userService = userService;
        this.messagingTemplate = messagingTemplate;
    }

    public void addUser(String username, String sessionId) {
        logger.info("Adding user {} with session {}", username, sessionId);
        userSessionMap.put(username, sessionId);
        userService.updateUserStatus(username, true);
        broadcastUserList();
    }

    public void removeUser(String username, String sessionId) {
        logger.info("Removing user {} with session {}", username, sessionId);
        // 只有当会话ID匹配时才移除用户
        String currentSessionId = userSessionMap.get(username);
        if (currentSessionId != null && currentSessionId.equals(sessionId)) {
            userSessionMap.remove(username);
            userService.updateUserStatus(username, false);
            broadcastUserList();
        } else {
            logger.info("Session ID mismatch for user {}. Current: {}, Disconnected: {}", 
                    username, currentSessionId, sessionId);
        }
    }

    public List<User> getOnlineUsers() {
        List<User> onlineUsers = new ArrayList<>();
        userSessionMap.keySet().forEach(username -> 
            userService.findByUsername(username).ifPresent(onlineUsers::add)
        );
        return onlineUsers;
    }

    public boolean isUserOnline(String username) {
        return userSessionMap.containsKey(username);
    }

    private void broadcastUserList() {
        List<User> onlineUsers = getOnlineUsers();
        logger.info("Broadcasting online users: {}", onlineUsers.size());
        messagingTemplate.convertAndSend("/topic/users", onlineUsers);
    }
} 