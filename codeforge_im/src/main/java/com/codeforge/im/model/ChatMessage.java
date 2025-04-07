package com.codeforge.im.model;

import com.codeforge.im.model.MessageType;
import lombok.Data;

@Data
public class ChatMessage {
    private String id;
    private String sender;
    private String receiver;
    private String content;
    private MessageType type;
    private String fileUrl;
    private String fileType;
    private Long fileSize;
    private String imageUrl;
    private Long timestamp;
    private boolean isRead;
    private String fileName;
    private boolean recalled;
}