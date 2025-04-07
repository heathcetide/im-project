package com.codeforge.im.controller;

import com.codeforge.im.model.User;
import com.codeforge.im.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpSession;

@Controller
public class AuthController {
    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute User user, Model model) {
        try {
            User registeredUser = userService.registerUser(user);
            return "redirect:/login?success=true";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }

    @PostMapping("/login")
    public String login(@ModelAttribute User user, Model model, HttpSession session) {
        return userService.findByUsername(user.getUsername())
                .filter(u -> u.getPassword().equals(user.getPassword()))
                .map(u -> {
                    session.setAttribute("username", u.getUsername());
                    return "redirect:/chat";
                })
                .orElseGet(() -> {
                    model.addAttribute("error", "Invalid username or password");
                    return "login";
                });
    }

    @GetMapping("/chat")
    public String chatPage(Model model, HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return "redirect:/login";
        }
        model.addAttribute("username", username);
        return "chat";
    }
} 