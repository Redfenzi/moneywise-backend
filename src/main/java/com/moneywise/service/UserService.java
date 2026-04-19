package com.moneywise.service;

import com.moneywise.dto.AuthResponse;
import com.moneywise.dto.PasswordChangeRequest;
import com.moneywise.dto.ProfileUpdateRequest;
import com.moneywise.entity.User;
import com.moneywise.repository.EmailVerificationTokenRepository;
import com.moneywise.repository.PasswordResetTokenRepository;
import com.moneywise.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Service
public class UserService {

    private static final Pattern STRONG_PASSWORD =
        Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z\\d]).{12,}$");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    public AuthResponse getProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        return new AuthResponse(null, user.getUsername(), user.getEmail(),
                user.getFirstName(), user.getLastName(), user.getUserType().name(), user.getCurrency());
    }

    public AuthResponse updateProfile(String username, ProfileUpdateRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (!user.getEmail().equals(request.getEmail())
                && userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Cet email est déjà utilisé");
        }

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        if (request.getCurrency() != null && !request.getCurrency().isBlank()) {
            user.setCurrency(request.getCurrency());
        }
        userRepository.save(user);

        return new AuthResponse(null, user.getUsername(), user.getEmail(),
                user.getFirstName(), user.getLastName(), user.getUserType().name(), user.getCurrency());
    }

    public void changePassword(String username, PasswordChangeRequest request) {
        if (!STRONG_PASSWORD.matcher(request.getNewPassword()).matches()) {
            throw new RuntimeException("Le mot de passe doit contenir au moins 12 caractères, une majuscule, une minuscule, un chiffre et un caractère spécial.");
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("Mot de passe actuel incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional
    public void deleteAccount(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Mot de passe incorrect");
        }

        passwordResetTokenRepository.deleteByUserId(user.getId());
        emailVerificationTokenRepository.deleteByUserId(user.getId());
        userRepository.delete(user);
    }
}
