package com.codeforge.im.model;

public enum MessageType {
    CHAT,       // 普通聊天消息
    JOIN,       // 用户加入
    LEAVE,      // 用户离开
    TYPING,     // 正在输入
    READ,       // 消息已读
    FILE,       // 文件消息
    IMAGE,      // 图片消息
    VIDEO,      // 视频消息
    SYSTEM,     // 系统消息
    RECALL      // 消息撤回
} 