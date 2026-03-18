package com.example.crm.service;

import com.example.crm.model.Users;
import com.example.crm.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.dao.DataIntegrityViolationException;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Users createUser(String username, String rawPassword, String role) {
        if (username == null || username.isBlank()) throw new IllegalArgumentException("username required");
        if (rawPassword == null || rawPassword.isBlank()) throw new IllegalArgumentException("password required");
        if (role == null || role.isBlank()) role = "USER";

        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("username already exists");
        }

        Users u = new Users();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode(rawPassword));
        u.setRole(role);
        try {
            return userRepository.save(u);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("username already exists");
        }
    }

    public Users authenticate(String username, String rawPassword) {
        return userRepository.findByUsername(username)
                .filter(u -> passwordEncoder.matches(rawPassword, u.getPassword()))
                .orElse(null);
    }

    public Users changePassword(String username, String oldPassword, String newPassword) {
        if (newPassword == null || newPassword.isBlank()) throw new IllegalArgumentException("newPassword required");
        Users u = userRepository.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("user not found"));
        if (!passwordEncoder.matches(oldPassword, u.getPassword())) throw new IllegalArgumentException("old password incorrect");
        u.setPassword(passwordEncoder.encode(newPassword));
        return userRepository.save(u);
    }

    public java.util.List<Users> getAllUsers() {
        return userRepository.findAll();
    }

    public void deleteUserById(Long id) {
        if (!userRepository.existsById(id)) throw new IllegalArgumentException("user not found");
        userRepository.deleteById(id);
    }

    public void deleteUserByUsername(String username) {
        java.util.Optional<Users> u = userRepository.findByUsername(username);
        if (u.isEmpty()) throw new IllegalArgumentException("user not found");
        userRepository.deleteByUsername(username);
    }
}
