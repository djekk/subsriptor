package com.noi.subscriptorapp.service;

import com.noi.subscriptorapp.model.EmailVerificationToken;
import com.noi.subscriptorapp.model.User;
import com.noi.subscriptorapp.repository.EmailVerificationTokenRepository;
import com.noi.subscriptorapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class EmailVerificationService {
    private static final int TOKEN_BYTES = 32;

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${email-verification.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${email-verification.from:no-reply@subscriptor.local}")
    private String fromEmail;

    @Value("${email-verification.expiration-hours:24}")
    private long expirationHours;

    public EmailVerificationService(JavaMailSender mailSender,
                                    UserRepository userRepository,
                                    EmailVerificationTokenRepository tokenRepository) {
        this.mailSender = mailSender;
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
    }

    @Transactional
    public void sendVerificationEmail(User user) {
        tokenRepository.deleteByUserId(user.getId());

        byte[] tokenBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(tokenBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        EmailVerificationToken token = new EmailVerificationToken(
                user.getId(),
                sha256(rawToken),
                LocalDateTime.now().plusHours(expirationHours)
        );
        tokenRepository.save(token);

        String verificationUrl = baseUrl.replaceAll("/$", "") + "/verify-email?token=" + rawToken;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(user.getEmail());
        message.setSubject("Verify your Alarm Service email");
        message.setText("Hello " + greetingName(user) + ",\n\n"
                + "Verify your email address by opening this link:\n"
                + verificationUrl + "\n\n"
                + "This link expires in " + expirationHours + " hours and can only be used once.");
        mailSender.send(message);
    }

    @Transactional
    public void verify(String rawToken) {
        if (rawToken == null || rawToken.trim().isEmpty()) {
            throw new IllegalArgumentException("Verification link is invalid");
        }

        EmailVerificationToken token = tokenRepository.findByTokenHash(sha256(rawToken.trim()))
                .orElseThrow(() -> new IllegalArgumentException("Verification link is invalid or expired"));
        if (token.getUsedAt() != null || token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Verification link is invalid or expired");
        }

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User for verification link was not found"));
        user.setIsActive(true);
        userRepository.save(user);
        token.setUsedAt(LocalDateTime.now());
        tokenRepository.save(token);
    }

    private String greetingName(User user) {
        return user.getFirstName() == null || user.getFirstName().trim().isEmpty()
                ? user.getUsername()
                : user.getFirstName().trim();
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hash = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                hash.append(String.format("%02x", item));
            }
            return hash.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
