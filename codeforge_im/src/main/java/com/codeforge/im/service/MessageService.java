package com.codeforge.im.service;

import com.codeforge.im.model.Message;
import com.codeforge.im.model.User;
import com.codeforge.im.repository.MessageRepository;
import com.codeforge.im.repository.UserRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.HashSet;

@Service
public class MessageService {
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    public MessageService(MessageRepository messageRepository, UserRepository userRepository) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    public Message saveMessage(Message message) {
        message.setCreatedAt(LocalDateTime.now());
        message.setRead(false);
        message.setRecalled(false);
        message.setReadBy(new HashSet<>());
        Message savedMessage = messageRepository.save(message);
        
        // Update user's message references
        userRepository.findById(message.getSenderId()).ifPresent(sender -> {
            sender.getSentMessageIds().add(savedMessage.getId());
            userRepository.save(sender);
        });
        
        if (message.getReceiverId() != null) {
            userRepository.findById(message.getReceiverId()).ifPresent(receiver -> {
                receiver.getReceivedMessageIds().add(savedMessage.getId());
                userRepository.save(receiver);
            });
        }
        
        return savedMessage;
    }

    public List<Message> getPrivateMessages(String senderUsername, String receiverUsername) {
        User sender = userRepository.findByUsername(senderUsername)
                .orElseThrow(() -> new RuntimeException("Sender not found"));
        User receiver = userRepository.findByUsername(receiverUsername)
                .orElseThrow(() -> new RuntimeException("Receiver not found"));
        return messageRepository.findBySenderIdAndReceiverIdOrderByCreatedAtAsc(sender.getId(), receiver.getId());
    }

    public List<Message> getPublicMessages() {
        return messageRepository.findByReceiverIdIsNullOrderByCreatedAtAsc();
    }

    public List<Message> getUnreadMessages(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return messageRepository.findByReceiverIdAndIsReadFalse(user.getId());
    }

    public void markMessagesAsRead(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<Message> unreadMessages = messageRepository.findByReceiverIdAndIsReadFalse(user.getId());
        unreadMessages.forEach(message -> {
            message.setRead(true);
            if (message.getReadBy() == null) {
                message.setReadBy(new HashSet<>());
            }
            message.getReadBy().add(user.getId());
            messageRepository.save(message);
        });
    }
    
    public Optional<Message> findById(String id) {
        return messageRepository.findById(id);
    }
    
    public boolean recallMessage(String messageId, String username) {
        Optional<Message> messageOpt = messageRepository.findById(messageId);
        if (!messageOpt.isPresent()) {
            return false;
        }
        
        Message message = messageOpt.get();
        
        // 检查消息是否在2分钟内发送
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime messageTime = message.getCreatedAt();
        long minutesDiff = java.time.Duration.between(messageTime, now).toMinutes();
        
        if (minutesDiff > 2) {
            return false;
        }
        
        // 检查是否是消息发送者
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (!message.getSenderId().equals(user.getId())) {
            return false;
        }
        
        // 标记消息为已撤回
        message.setRecalled(true);
        message.setRecallTime(now);
        messageRepository.save(message);
        
        return true;
    }
    
    public void markMessageAsRead(String messageId, String username) {
        Optional<Message> messageOpt = messageRepository.findById(messageId);
        if (!messageOpt.isPresent()) {
            return;
        }
        
        Message message = messageOpt.get();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (message.getReadBy() == null) {
            message.setReadBy(new HashSet<>());
        }
        
        if (!message.getReadBy().contains(user.getId())) {
            message.getReadBy().add(user.getId());
            message.setRead(true);
            messageRepository.save(message);
        }
    }
    
    public List<Message> searchMessages(String username, String keyword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // 搜索用户发送和接收的消息
        return messageRepository.findByContentContainingAndSenderIdOrContentContainingAndReceiverId(
                keyword, user.getId(), keyword, user.getId());
    }
} 