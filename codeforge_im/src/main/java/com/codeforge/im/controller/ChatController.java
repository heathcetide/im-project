package com.codeforge.im.controller;

import com.codeforge.im.model.ChatMessage;
import com.codeforge.im.model.Message;
import com.codeforge.im.model.MessageType;
import com.codeforge.im.model.User;
import com.codeforge.im.service.MessageService;
import com.codeforge.im.service.OnlineUserService;
import com.codeforge.im.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class ChatController {
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    
    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;
    private final UserService userService;
    private final OnlineUserService onlineUserService;

    public ChatController(SimpMessagingTemplate messagingTemplate,
                         MessageService messageService,
                         UserService userService,
                         OnlineUserService onlineUserService) {
        this.messagingTemplate = messagingTemplate;
        this.messageService = messageService;
        this.userService = userService;
        this.onlineUserService = onlineUserService;
    }

    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    public ChatMessage sendMessage(@Payload ChatMessage chatMessage) {
        logger.info("Received message: {}", chatMessage);
        
        // 保存消息到数据库
        Message message = new Message();
        message.setContent(chatMessage.getContent());
        message.setType(chatMessage.getType());
        
        User sender = userService.findByUsername(chatMessage.getSender())
                .orElseThrow(() -> new RuntimeException("Sender not found: " + chatMessage.getSender()));
        message.setSenderId(sender.getId());
        
        Message savedMessage = messageService.saveMessage(message);
        chatMessage.setId(savedMessage.getId());
        
        return chatMessage;
    }

    @MessageMapping("/chat.sendPrivateMessage")
    public void sendPrivateMessage(@Payload ChatMessage chatMessage) {
        logger.info("Received private message: {}", chatMessage);
        
        // 保存消息到数据库
        Message message = new Message();
        message.setContent(chatMessage.getContent());
        message.setType(chatMessage.getType());
        
        User sender = userService.findByUsername(chatMessage.getSender())
                .orElseThrow(() -> new RuntimeException("Sender not found: " + chatMessage.getSender()));
        User receiver = userService.findByUsername(chatMessage.getReceiver())
                .orElseThrow(() -> new RuntimeException("Receiver not found: " + chatMessage.getReceiver()));
        
        message.setSenderId(sender.getId());
        message.setReceiverId(receiver.getId());
        
        // 如果是文件消息，添加文件名和类型
        if (chatMessage.getType() == MessageType.FILE) {
            message.setFileName(chatMessage.getFileName());
            message.setFileType(chatMessage.getFileType());
        }
        
        Message savedMessage = messageService.saveMessage(message);
        chatMessage.setId(savedMessage.getId());

        // 发送消息给接收者
        messagingTemplate.convertAndSendToUser(
                chatMessage.getReceiver(),
                "/queue/private",
                chatMessage
        );
        
        // 发送消息给发送者自己
        messagingTemplate.convertAndSendToUser(
                chatMessage.getSender(),
                "/queue/private",
                chatMessage
        );
        
        logger.info("Private message sent to {} and {}", chatMessage.getReceiver(), chatMessage.getSender());
    }

    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public ChatMessage addUser(@Payload ChatMessage chatMessage,
                              SimpMessageHeaderAccessor headerAccessor) {
        String username = headerAccessor.getSessionAttributes().get("username") != null ?
                (String) headerAccessor.getSessionAttributes().get("username") :
                chatMessage.getSender();
                
        if (username == null) {
            throw new RuntimeException("Username not found in session");
        }
        
        headerAccessor.getSessionAttributes().put("username", username);
        onlineUserService.addUser(username, headerAccessor.getSessionId());
        
        ChatMessage joinMessage = new ChatMessage();
        joinMessage.setType(MessageType.JOIN);
        joinMessage.setSender(username);
        return joinMessage;
    }

    @MessageMapping("/chat.typing")
    public void typing(@Payload ChatMessage chatMessage) {
        if (chatMessage.getReceiver() != null) {
            messagingTemplate.convertAndSendToUser(
                    chatMessage.getReceiver(),
                    "/queue/typing",
                    chatMessage
            );
        }
    }

    @MessageMapping("/chat.read")
    public void markAsRead(@Payload ChatMessage chatMessage) {
        messageService.markMessagesAsRead(chatMessage.getSender());
        if (chatMessage.getReceiver() != null) {
            messagingTemplate.convertAndSendToUser(
                    chatMessage.getReceiver(),
                    "/queue/read",
                    chatMessage
            );
        }
    }
    
    @MessageMapping("/chat.readMessage")
    public void markMessageAsRead(@Payload ChatMessage chatMessage) {
        logger.info("Marking message as read: {}", chatMessage.getId());
        messageService.markMessageAsRead(chatMessage.getId(), chatMessage.getSender());
        
        // 通知发送者消息已被读取
        if (chatMessage.getReceiver() != null) {
            messagingTemplate.convertAndSendToUser(
                    chatMessage.getSender(),
                    "/queue/read",
                    chatMessage
            );
        }
    }

    @MessageMapping("/chat.history")
    public void getChatHistory(@Payload ChatMessage chatMessage) {
        logger.info("Received history request: {}", chatMessage);
        
        List<Message> messages;
        if (chatMessage.getReceiver() != null) {
            logger.info("Loading private messages between {} and {}", chatMessage.getSender(), chatMessage.getReceiver());
            messages = messageService.getPrivateMessages(chatMessage.getSender(), chatMessage.getReceiver());
        } else {
            logger.info("Loading public messages");
            messages = messageService.getPublicMessages();
        }
        
        logger.info("Found {} messages", messages.size());
        
        // 转换消息格式
        List<ChatMessage> chatMessages = messages.stream()
            .map(message -> {
                ChatMessage chatMsg = new ChatMessage();
                chatMsg.setId(message.getId());
                chatMsg.setContent(message.getContent());
                chatMsg.setType(message.getType());
                chatMsg.setRecalled(message.isRecalled());
                
                // 获取发送者用户名
                User sender = userService.findById(message.getSenderId())
                    .orElseThrow(() -> new RuntimeException("Sender not found: " + message.getSenderId()));
                chatMsg.setSender(sender.getUsername());
                
                // 如果有接收者，获取接收者用户名
                if (message.getReceiverId() != null) {
                    User receiver = userService.findById(message.getReceiverId())
                        .orElseThrow(() -> new RuntimeException("Receiver not found: " + message.getReceiverId()));
                    chatMsg.setReceiver(receiver.getUsername());
                }
                
                // 如果是文件消息，添加文件名和类型
                if (message.getType() == MessageType.FILE) {
                    chatMsg.setFileName(message.getFileName());
                    chatMsg.setFileType(message.getFileType());
                }
                
                return chatMsg;
            })
            .collect(Collectors.toList());
        
        logger.info("Sending {} chat messages to {}", chatMessages.size(), chatMessage.getSender());
        
        messagingTemplate.convertAndSendToUser(
                chatMessage.getSender(),
                "/queue/history",
                chatMessages
        );
    }
    
    @MessageMapping("/chat.recallMessage")
    @SendTo("/topic/public")
    public ChatMessage recallMessage(@Payload ChatMessage chatMessage) {
        logger.info("Received recall request for message: {}", chatMessage.getId());
        
        boolean recalled = messageService.recallMessage(chatMessage.getId(), chatMessage.getSender());
        
        if (recalled) {
            chatMessage.setRecalled(true);
            return chatMessage;
        } else {
            // 如果撤回失败，发送错误消息给发送者
            ChatMessage errorMessage = new ChatMessage();
            errorMessage.setType(MessageType.SYSTEM);
            errorMessage.setContent("Failed to recall message. Message may be too old or you may not have permission.");
            errorMessage.setSender("System");
            
            messagingTemplate.convertAndSendToUser(
                    chatMessage.getSender(),
                    "/queue/private",
                    errorMessage
            );
            
            return null;
        }
    }
    
    @MessageMapping("/chat.recallPrivateMessage")
    public void recallPrivateMessage(@Payload ChatMessage chatMessage) {
        logger.info("Received recall request for private message: {}", chatMessage.getId());
        
        boolean recalled = messageService.recallMessage(chatMessage.getId(), chatMessage.getSender());
        
        if (recalled) {
            chatMessage.setRecalled(true);
            
            // 发送撤回通知给接收者
            messagingTemplate.convertAndSendToUser(
                    chatMessage.getReceiver(),
                    "/queue/private",
                    chatMessage
            );
            
            // 发送撤回通知给发送者
            messagingTemplate.convertAndSendToUser(
                    chatMessage.getSender(),
                    "/queue/private",
                    chatMessage
            );
        } else {
            // 如果撤回失败，发送错误消息给发送者
            ChatMessage errorMessage = new ChatMessage();
            errorMessage.setType(MessageType.SYSTEM);
            errorMessage.setContent("Failed to recall message. Message may be too old or you may not have permission.");
            errorMessage.setSender("System");
            
            messagingTemplate.convertAndSendToUser(
                    chatMessage.getSender(),
                    "/queue/private",
                    errorMessage
            );
        }
    }
    
    @MessageMapping("/chat.search")
    public void searchMessages(@Payload ChatMessage chatMessage) {
        logger.info("Received search request with keyword: {}", chatMessage.getContent());
        
        List<Message> messages = messageService.searchMessages(chatMessage.getSender(), chatMessage.getContent());
        
        // 转换消息格式
        List<ChatMessage> chatMessages = messages.stream()
            .map(message -> {
                ChatMessage chatMsg = new ChatMessage();
                chatMsg.setId(message.getId());
                chatMsg.setContent(message.getContent());
                chatMsg.setType(message.getType());
                chatMsg.setRecalled(message.isRecalled());
                
                // 获取发送者用户名
                User sender = userService.findById(message.getSenderId())
                    .orElseThrow(() -> new RuntimeException("Sender not found: " + message.getSenderId()));
                chatMsg.setSender(sender.getUsername());
                
                // 如果有接收者，获取接收者用户名
                if (message.getReceiverId() != null) {
                    User receiver = userService.findById(message.getReceiverId())
                        .orElseThrow(() -> new RuntimeException("Receiver not found: " + message.getReceiverId()));
                    chatMsg.setReceiver(receiver.getUsername());
                }
                
                // 如果是文件消息，添加文件名和类型
                if (message.getType() == MessageType.FILE) {
                    chatMsg.setFileName(message.getFileName());
                    chatMsg.setFileType(message.getFileType());
                }
                
                return chatMsg;
            })
            .collect(Collectors.toList());
        
        logger.info("Found {} messages matching search criteria", chatMessages.size());
        
        messagingTemplate.convertAndSendToUser(
                chatMessage.getSender(),
                "/queue/search",
                chatMessages
        );
    }
}