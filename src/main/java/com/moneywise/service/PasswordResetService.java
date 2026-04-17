package com.moneywise.service;

import com.moneywise.entity.PasswordResetToken;
import com.moneywise.entity.User;
import com.moneywise.repository.PasswordResetTokenRepository;
import com.moneywise.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PasswordResetService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository resetTokenRepository;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    /**
     * Génère un token de reset et un browserKey lié au navigateur demandeur.
     * Retourne le browserKey à stocker dans le localStorage du frontend.
     * Ne révèle jamais si l'email existe ou non (sécurité anti-enumération).
     */
    public String requestReset(String email) {
        // Nettoyage des tokens expirés
        resetTokenRepository.deleteExpired(LocalDateTime.now());

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            // Délai simulé pour éviter l'énumération d'emails
            return UUID.randomUUID().toString();
        }

        // Supprime les anciens tokens de cet utilisateur
        resetTokenRepository.deleteByUserId(user.getId());

        String token = UUID.randomUUID().toString();
        String browserKey = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);

        resetTokenRepository.save(new PasswordResetToken(token, browserKey, user, expiresAt));

        sendResetEmail(user, token, browserKey);

        return browserKey;
    }

    /**
     * Réinitialise le mot de passe si le token ET le browserKey sont valides.
     * Le browserKey doit correspondre à celui stocké dans le localStorage du navigateur demandeur.
     */
    public void resetPassword(String token, String browserKey, String newPassword) {
        PasswordResetToken resetToken = resetTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Lien de réinitialisation invalide ou expiré."));

        if (resetToken.isUsed()) {
            throw new RuntimeException("Ce lien a déjà été utilisé.");
        }
        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            resetTokenRepository.delete(resetToken);
            throw new RuntimeException("Le lien de réinitialisation a expiré.");
        }
        if (!resetToken.getBrowserKey().equals(browserKey)) {
            throw new RuntimeException("Ce lien ne peut être utilisé que depuis le navigateur d'origine.");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        resetTokenRepository.save(resetToken);
    }

    private void sendResetEmail(User user, String token, String browserKey) {
        String resetUrl = frontendUrl + "/auth/reset-password?token=" + token + "&bk=" + browserKey;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("redtechsolutions75@gmail.com");
        message.setTo(user.getEmail());
        message.setSubject("MoneyWise - Réinitialisation de votre mot de passe");
        message.setText(
            "Bonjour " + user.getFirstName() + ",\n\n" +
            "Vous avez demandé la réinitialisation de votre mot de passe MoneyWise.\n\n" +
            "Cliquez sur le lien ci-dessous pour définir un nouveau mot de passe :\n\n" +
            resetUrl + "\n\n" +
            "⚠️ Ce lien est valable 1 heure.\n\n" +
            "Si vous n'êtes pas à l'origine de cette demande, ignorez cet email.\n\n" +
            "— L'équipe MoneyWise"
        );

        mailSender.send(message);
    }
}
