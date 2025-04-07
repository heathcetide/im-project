package com.codeforge.im.model;

import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Document(collection = "messages")
public class Message {
    @Id
    private String id;

    @Field("sender_id")
    private String senderId;

    @Field("receiver_id")
    private String receiverId;

    @Field("content")
    private String content;

    @Field("message_type")
    private MessageType type;

    @Field("created_at")
    private LocalDateTime createdAt;

    @Field("is_read")
    private boolean isRead;

    @Field("read_by")
    private Set<String> readBy;

    @Field("file_name")
    private String fileName;

    @Field("file_type")
    private String fileType;

    @Field("is_recalled")
    private boolean isRecalled;

    @Field("recall_time")
    private LocalDateTime recallTime;
} 