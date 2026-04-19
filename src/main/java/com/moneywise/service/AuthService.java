package com.moneywise.service;

import com.moneywise.dto.*;
import com.moneywise.entity.User;
import com.moneywise.repository.UserRepository;
import com.moneywise.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class AuthService {

    private static final Pattern STRONG_PASSWORD =
        Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z\\d]).{12,}$");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private EmailVerificationService emailVerificationService;

    public void register(RegisterRequest request) {
        if (!STRONG_PASSWORD.matcher(request.getPassword()).matches()) {
            throw new RuntimeException("Le mot de passe doit contenir au moins 12 caractères, une majuscule, une minuscule, un chiffre et un caractère spécial.");
        }
        // Si l'email existe mais le compte n'est pas vérifié → on supprime pour autoriser la réinscription
        User existingByEmail = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (existingByEmail != null) {
            if (existingByEmail.isEmailVerified()) {
                throw new RuntimeException("Cet email est déjà utilisé");
            }
            userRepository.delete(existingByEmail);
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Ce nom d'utilisateur est déjà pris");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setUserType(User.UserType.valueOf(request.getUserType().toUpperCase()));
        if (request.getCurrency() != null && !request.getCurrency().isBlank()) {
            user.setCurrency(request.getCurrency());
        }
        user.setEmailVerified(false);

        userRepository.save(user);
        emailVerificationService.sendVerificationEmail(user);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (!user.isEmailVerified()) {
            throw new RuntimeException("EMAIL_NOT_VERIFIED");
        }

        String token = jwtUtils.generateToken(user.getUsername());
        return new AuthResponse(token, user.getUsername(), user.getEmail(),
                user.getFirstName(), user.getLastName(), user.getUserType().name(), user.getCurrency());
    }
}
