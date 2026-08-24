package com.noi.subscriptorapp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noi.subscriptorapp.model.Order;
import com.noi.subscriptorapp.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StripePaymentServiceTest {
    private static final String REQUEST_KEY = "1CFA7B20EE3D9B48227DBAA6739AE044";
    private static final String RESPONSE_KEY = "DE0216CEF066E18B5AC877A43E982EF6";

    private OrderService orderService;
    private StripePaymentService stripePaymentService;

    @BeforeEach
    void setUp() {
        ProductRepository productRepository = mock(ProductRepository.class);
        orderService = mock(OrderService.class);
        stripePaymentService = new StripePaymentService(productRepository, orderService, new ObjectMapper());
        ReflectionTestUtils.setField(stripePaymentService, "subscriptionCheckRequestAesKey", REQUEST_KEY);
        ReflectionTestUtils.setField(stripePaymentService, "subscriptionCheckResponseAesKey", RESPONSE_KEY);
        stripePaymentService.init();
    }

    @Test
    void resolvesEncryptedDeviceToken() {
        String deviceNumber = stripePaymentService.resolveSubscriptionCheckDeviceNumber("33D64B41C794CD8CA6436CB2C53C035F");

        assertEquals("14D84D72D632AE5C", deviceNumber);
    }

    @Test
    void buildsEncryptedSubscriptionPayloadForLatestPaidSubscription() throws Exception {
        LocalDateTime olderCreatedAt = LocalDateTime.of(2026, 1, 1, 10, 0, 0);
        LocalDateTime latestCreatedAt = LocalDateTime.of(2026, 8, 24, 9, 0, 0);
        Order olderOrder = paidSubscription("SUBSCRIPTION_6M", olderCreatedAt);
        Order latestOrder = paidSubscription("SUBSCRIPTION_1Y", latestCreatedAt);
        when(orderService.findPaidSubscriptionsByDevice("14D84D72D632AE5C"))
                .thenReturn(Arrays.asList(olderOrder, latestOrder));

        StripePaymentService.SubscriptionProtocolResponse response =
                stripePaymentService.checkSubscriptionByDevice("33D64B41C794CD8CA6436CB2C53C035F");

        assertTrue(response.isFound());
        assertEquals("14D84D72D632AE5C", response.getDeviceNumber());

        byte[] decryptedPayload = decrypt(response.getPayload(), RESPONSE_KEY);
        long createdAtUnix = latestCreatedAt.atZone(ZoneId.systemDefault()).toEpochSecond();
        long expiresAtUnix = latestCreatedAt.plusMonths(12).atZone(ZoneId.systemDefault()).toEpochSecond();
        String expectedPlainHex = "14D84D72D632AE5C"
                + String.format("%08X", (int) createdAtUnix)
                + String.format("%08X", (int) expiresAtUnix);

        assertEquals(expectedPlainHex, toHex(decryptedPayload));
    }

    @Test
    void returnsEmptyPayloadWhenPaidSubscriptionDoesNotExist() {
        when(orderService.findPaidSubscriptionsByDevice("14D84D72D632AE5C"))
                .thenReturn(Collections.<Order>emptyList());

        StripePaymentService.SubscriptionProtocolResponse response =
                stripePaymentService.checkSubscriptionByDevice("33D64B41C794CD8CA6436CB2C53C035F");

        assertFalse(response.isFound());
        assertEquals(StripePaymentService.EMPTY_SUBSCRIPTION_STATUS, response.getPayload());
    }

    @Test
    void rejectsEncryptedDeviceTokenWhenHalvesDoNotMatch() {
        assertThrows(IllegalArgumentException.class,
                () -> stripePaymentService.resolveSubscriptionCheckDeviceNumber("E7513AAB26A5392F7DFFB58FACD486D7"));
    }

    private Order paidSubscription(String productCode, LocalDateTime createdAt) {
        Order order = new Order();
        order.setProductCode(productCode);
        order.setCreatedAt(createdAt);
        return order;
    }

    private byte[] decrypt(String hexPayload, String hexKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(hexToBytes(hexKey), "AES"));
        return cipher.doFinal(hexToBytes(hexPayload));
    }

    private byte[] hexToBytes(String value) {
        byte[] bytes = new byte[value.length() / 2];
        for (int i = 0; i < value.length(); i += 2) {
            bytes[i / 2] = (byte) ((Character.digit(value.charAt(i), 16) << 4)
                    + Character.digit(value.charAt(i + 1), 16));
        }
        return bytes;
    }

    private String toHex(byte[] value) {
        StringBuilder builder = new StringBuilder(value.length * 2);
        for (byte b : value) {
            builder.append(String.format("%02X", b & 0xFF));
        }
        return builder.toString();
    }
}
