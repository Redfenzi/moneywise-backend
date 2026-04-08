package com.moneywise.controller;

import com.moneywise.dto.AuthResponse;
import com.moneywise.dto.PasswordChangeRequest;
import com.moneywise.dto.ProfileUpdateRequest;
import com.moneywise.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<AuthResponse> getProfile(Authentication auth) {
        return ResponseEntity.ok(userService.getProfile(auth.getName()));
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(Authentication auth,
                                           @Valid @RequestBody ProfileUpdateRequest request) {
        try {
            AuthResponse response = userService.updateProfile(auth.getName(), request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/password")
    public ResponseEntity<?> changePassword(Authentication auth,
                                            @Valid @RequestBody PasswordChangeRequest request) {
        try {
            userService.changePassword(auth.getName(), request);
            return ResponseEntity.ok(Map.of("message", "Mot de passe modifié avec succès"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/account")
    public ResponseEntity<?> deleteAccount(Authentication auth,
                                           @RequestBody Map<String, String> body) {
        try {
            userService.deleteAccount(auth.getName(), body.get("password"));
            return ResponseEntity.ok(Map.of("message", "Compte supprimé avec succès"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
