package com.moneywise.service;

import com.moneywise.entity.EmailVerificationToken;
import com.moneywise.entity.User;
import com.moneywise.repository.EmailVerificationTokenRepository;
import com.moneywise.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class EmailVerificationService {

    @Autowired
    private EmailVerificationTokenRepository tokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    /** Génère et envoie l'email de confirmation pour un nouvel inscrit. */
    public void sendVerificationEmail(User user) {
        tokenRepository.deleteExpired(LocalDateTime.now());
        tokenRepository.deleteByUserId(user.getId());

        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);

        tokenRepository.save(new EmailVerificationToken(token, user, expiresAt));
        sendEmail(user, token);
    }

    /** Vérifie le token et active le compte. Lance une exception si invalide/expiré. */
    public void verifyEmail(String token) {
        EmailVerificationToken verificationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Lien de vérification invalide ou expiré."));

        if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            tokenRepository.delete(verificationToken);
            throw new RuntimeException("Le lien de vérification a expiré. Veuillez vous réinscrire.");
        }

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        tokenRepository.delete(verificationToken);
    }

    private void sendEmail(User user, String token) {
        String verifyUrl = frontendUrl + "/auth/verify-email?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("redtechsolutions75@gmail.com");
        message.setTo(user.getEmail());
        message.setSubject("MoneyWise - Confirmez votre adresse email");
        message.setText(
            "Bonjour " + user.getFirstName() + ",\n\n" +
            "Merci de vous être inscrit sur MoneyWise !\n\n" +
            "Pour activer votre compte, cliquez sur le lien ci-dessous :\n\n" +
            verifyUrl + "\n\n" +
            "⚠️ Ce lien est valable 24 heures.\n\n" +
            "Si vous n'avez pas créé de compte, ignorez cet email.\n\n" +
            "— L'équipe MoneyWise"
        );

        mailSender.send(message);
    }
}
