package com.codeforge.im.service;

import com.codeforge.im.model.User;
import com.codeforge.im.repository.UserRepository;
import org.springframework.stereotype.Service;
import java.util.Optional;
import java.util.HashSet;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User registerUser(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        user.setOnline(false);
        user.setSentMessageIds(new HashSet<>());
        user.setReceivedMessageIds(new HashSet<>());
        return userRepository.save(user);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findById(String id) {
        return userRepository.findById(id);
    }

    public void updateUserStatus(String username, boolean isOnline) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setOnline(isOnline);
            userRepository.save(user);
        });
    }

    public User saveUser(User user) {
        return userRepository.save(user);
    }
} 