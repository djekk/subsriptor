package com.noi.subscriptorapp.service;

import com.noi.subscriptorapp.model.Order;
import com.noi.subscriptorapp.model.User;
import com.noi.subscriptorapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderConfirmationEmailServiceTest {
    private JavaMailSender mailSender;
    private UserRepository userRepository;
    private OrderConfirmationEmailService service;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        userRepository = mock(UserRepository.class);
        service = new OrderConfirmationEmailService(mailSender, userRepository);
        ReflectionTestUtils.setField(service, "confirmationEmailEnabled", true);
        ReflectionTestUtils.setField(service, "fromEmail", "no-reply@subscriptor.local");
    }

    @Test
    void sendsCustomerEmailWithHiddenAdminRecipients() {
        User user = new User();
        user.setId(7L);
        user.setEmail("customer@example.com");
        user.setFirstName("John");
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        ReflectionTestUtils.setField(service, "adminEmails",
                "admin1@example.com; admin2@example.com, admin3@example.com");

        service.sendOrderConfirmation(createOrder());

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();

        assertArrayEquals(new String[]{"customer@example.com"}, message.getTo());
        assertArrayEquals(new String[]{
                "admin1@example.com",
                "admin2@example.com",
                "admin3@example.com"
        }, message.getBcc());
        assertEquals("no-reply@subscriptor.local", message.getFrom());
    }

    private Order createOrder() {
        Order order = new Order();
        order.setUserId(7L);
        order.setOrderNumber("ORD-100");
        order.setUsername("john");
        order.setProductName("Subscription");
        order.setProductVariant("12M");
        order.setQuantity(1);
        order.setUnitPrice(new BigDecimal("19.99"));
        order.setTotalAmount(new BigDecimal("19.99"));
        order.setDeviceNumber("DEVICE-1");
        order.setPaymentProvider("stripe");
        order.setStatus("PAID");
        order.setCreatedAt(LocalDateTime.of(2026, 8, 27, 12, 0, 0));
        return order;
    }
}
