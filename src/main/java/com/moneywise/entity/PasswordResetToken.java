package com.moneywise.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_tokens")
@Data
@NoArgsConstructor
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String token;

    /** Clé stockée dans le localStorage du navigateur demandeur */
    @Column(nullable = false)
    private String browserKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean used = false;

    public PasswordResetToken(String token, String browserKey, User user, LocalDateTime expiresAt) {
        this.token = token;
        this.browserKey = browserKey;
        this.user = user;
        this.expiresAt = expiresAt;
    }
}
