package com.example.crm.controller;

import com.example.crm.model.Users;
import com.example.crm.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public static class RegisterRequest {
        public String username;
        public String password;
        public String role;
    }

    public static class LoginRequest {
        public String username;
        public String password;
    }

    public static class ChangePasswordRequest {
        public String username;
        public String oldPassword;
        public String newPassword;
    }

    public static class UserResponse {
        public Long id;
        public String username;
        public String role;
        public UserResponse(Long id, String username, String role) { this.id = id; this.username = username; this.role = role; }
    }

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        try {
            Users u = userService.createUser(req.username, req.password, req.role);
            return ResponseEntity.ok().body(Map.of("id", u.getId(), "username", u.getUsername(), "role", u.getRole()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        Users u = userService.authenticate(req.username, req.password);
        if (u == null) return ResponseEntity.status(401).body(Map.of("error", "invalid credentials"));
        return ResponseEntity.ok().body(Map.of("id", u.getId(), "username", u.getUsername(), "role", u.getRole()));
    }

    @PutMapping(value = "/change-password", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest req) {
        try {
            Users u = userService.changePassword(req.username, req.oldPassword, req.newPassword);
            return ResponseEntity.ok().body(Map.of("id", u.getId(), "username", u.getUsername()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @org.springframework.web.bind.annotation.GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> listUsers() {
        java.util.List<Users> users = userService.getAllUsers();
        java.util.List<UserResponse> out = new java.util.ArrayList<>();
        for (Users u : users) {
            out.add(new UserResponse(u.getId(), u.getUsername(), u.getRole()));
        }
        return ResponseEntity.ok().body(out);
    }

    @org.springframework.web.bind.annotation.DeleteMapping(path = "/{id}")
    public ResponseEntity<?> deleteUserById(@org.springframework.web.bind.annotation.PathVariable("id") Long id) {
        try {
            userService.deleteUserById(id);
            return ResponseEntity.ok().body(Map.of("deletedId", id));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    @org.springframework.web.bind.annotation.DeleteMapping(value = "", params = "username")
    public ResponseEntity<?> deleteUserByUsername(@org.springframework.web.bind.annotation.RequestParam("username") String username) {
        try {
            userService.deleteUserByUsername(username);
            return ResponseEntity.ok().body(Map.of("deletedUsername", username));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }
}
