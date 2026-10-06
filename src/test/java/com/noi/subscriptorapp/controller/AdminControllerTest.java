package com.noi.subscriptorapp.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.noi.subscriptorapp.model.Order;
import com.noi.subscriptorapp.model.User;
import com.noi.subscriptorapp.repository.UserRepository;
import com.noi.subscriptorapp.service.OrderService;
import com.noi.subscriptorapp.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminControllerTest {
    private final OrderService orderService = mock(OrderService.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final AdminController controller = new AdminController(
            orderService, mock(ProductService.class), userRepository);

    @Test
    void includesEmailWithExistingOrderFieldsAndHandlesMissingUsers() {
        Order first = order(7L, "ORD-1");
        Order second = order(7L, "ORD-2");
        Order missingUser = order(8L, "ORD-3");
        Order anonymous = order(null, "ORD-4");
        User user = new User();
        user.setId(7L);
        user.setEmail("customer@example.com");
        when(orderService.findAllOrdersByCreatedAtDesc())
                .thenReturn(Arrays.asList(first, second, missingUser, anonymous));
        when(userRepository.findAllById(Arrays.asList(7L, 8L)))
                .thenReturn(Collections.singletonList(user));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 1L);
        session.setAttribute("role", "ADMIN");

        JsonNode data = new ObjectMapper().valueToTree(controller.getOrders(session).getBody().getData());

        assertEquals(4, data.size());
        assertEquals("ORD-1", data.get(0).get("orderNumber").asText());
        assertEquals("DEVICE-1", data.get(0).get("deviceNumber").asText());
        assertEquals("customer@example.com", data.get(0).get("email").asText());
        assertEquals("customer@example.com", data.get(1).get("email").asText());
        assertTrue(data.get(2).get("email").isNull());
        assertTrue(data.get(3).get("email").isNull());
        assertFalse(data.get(0).has("order"));
        assertFalse(data.get(0).has("password"));
        verify(userRepository).findAllById(Arrays.asList(7L, 8L));
    }

    @Test
    void doesNotLoadOrdersOrEmailsWithoutAdminAccess() {
        MockHttpSession session = new MockHttpSession();
        assertEquals(401, controller.getOrders(session).getStatusCodeValue());
        session.setAttribute("userId", 2L);
        session.setAttribute("role", "USER");
        assertEquals(403, controller.getOrders(session).getStatusCodeValue());
        verifyNoInteractions(orderService, userRepository);
    }

    private Order order(Long userId, String number) {
        Order order = new Order();
        order.setUserId(userId);
        order.setOrderNumber(number);
        order.setDeviceNumber("DEVICE-1");
        return order;
    }
}
