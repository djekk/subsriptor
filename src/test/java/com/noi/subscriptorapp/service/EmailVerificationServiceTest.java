package com.noi.subscriptorapp.service;

import com.noi.subscriptorapp.model.EmailVerificationToken;
import com.noi.subscriptorapp.model.User;
import com.noi.subscriptorapp.repository.EmailVerificationTokenRepository;
import com.noi.subscriptorapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmailVerificationServiceTest {
    private JavaMailSender mailSender;
    private UserRepository userRepository;
    private EmailVerificationTokenRepository tokenRepository;
    private EmailVerificationService service;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        userRepository = mock(UserRepository.class);
        tokenRepository = mock(EmailVerificationTokenRepository.class);
        service = new EmailVerificationService(mailSender, userRepository, tokenRepository);
        ReflectionTestUtils.setField(service, "baseUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(service, "fromEmail", "no-reply@example.com");
        ReflectionTestUtils.setField(service, "expirationHours", 24L);
    }

    @Test
    void sendsSingleUseVerificationLinkAndStoresOnlyHash() {
        User user = new User();
        user.setId(7L);
        user.setUsername("john");
        user.setEmail("john@example.com");
        when(tokenRepository.save(any(EmailVerificationToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.sendVerificationEmail(user);

        verify(tokenRepository).deleteByUserId(7L);
        verify(mailSender).send(any(SimpleMailMessage.class));
        verify(tokenRepository).save(argThat(token ->
                token.getTokenHash().length() == 64
                        && token.getExpiresAt().isAfter(LocalDateTime.now())));
    }

    @Test
    void rejectsUnknownVerificationToken() {
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.verify("unknown-token"));
        verifyNoInteractions(userRepository);
    }
}
