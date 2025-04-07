package com.codeforge.im.repository;

import com.codeforge.im.model.Message;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface MessageRepository extends MongoRepository<Message, String> {
    List<Message> findBySenderIdAndReceiverIdOrderByCreatedAtAsc(String senderId, String receiverId);
    List<Message> findByReceiverIdIsNullOrderByCreatedAtAsc();
    List<Message> findByReceiverIdAndIsReadFalse(String receiverId);
    List<Message> findByContentContainingAndSenderIdOrContentContainingAndReceiverId(String content1, String senderId, String content2, String receiverId);
} 