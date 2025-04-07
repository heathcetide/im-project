package com.codeforge.im.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.util.Set;

@Data
@Document(collection = "users")
public class User {
    @Id
    private String id;

    @Field("username")
    private String username;

    @Field("password")
    private String password;

    @Field("email")
    private String email;

    @Field("is_online")
    private boolean isOnline;

    @Field("sent_messages")
    private Set<String> sentMessageIds;

    @Field("received_messages")
    private Set<String> receivedMessageIds;
} 