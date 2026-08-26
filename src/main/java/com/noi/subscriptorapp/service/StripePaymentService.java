package com.noi.subscriptorapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.noi.subscriptorapp.dto.StripeCheckoutRequest;
import com.noi.subscriptorapp.model.Order;
import com.noi.subscriptorapp.model.Product;
import com.noi.subscriptorapp.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class StripePaymentService {
    private static final long WEBHOOK_TOLERANCE_SECONDS = 300;
    private static final int SERIAL_LEN = 16;
    private static final int PROTOCOL_DEVICE_TOKEN_LEN = 32;
    private static final int BINARY_SERIAL_LEN = 8;
    public static final String EMPTY_SUBSCRIPTION_STATUS = "00000000000000000000000000000000";

    private final ProductRepository productRepository;
    private final OrderService orderService;
    private final OrderConfirmationEmailService orderConfirmationEmailService;
    private final ObjectMapper objectMapper;

    @Value("${stripe.secret-key:}")
    private String secretKey;

    @Value("${stripe.webhook-secret:}")
    private String webhookSecret;

    @Value("${stripe.success-url}")
    private String successUrl;

    @Value("${stripe.cancel-url}")
    private String cancelUrl;

    @Value("${subscription-check.request-aes-key}")
    private String subscriptionCheckRequestAesKey;

    @Value("${subscription-check.response-aes-key}")
    private String subscriptionCheckResponseAesKey;

    public StripePaymentService(
            ProductRepository productRepository,
            OrderService orderService,
            OrderConfirmationEmailService orderConfirmationEmailService,
            ObjectMapper objectMapper) {
        this.productRepository = productRepository;
        this.orderService = orderService;
        this.orderConfirmationEmailService = orderConfirmationEmailService;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        validateAesKey(subscriptionCheckRequestAesKey, "subscription-check.request-aes-key");
        validateAesKey(subscriptionCheckResponseAesKey, "subscription-check.response-aes-key");
    }

    public CheckoutResult createCheckoutSession(StripeCheckoutRequest request, Long userId, String username) throws Exception {
        if (secretKey == null || secretKey.trim().isEmpty()) {
            throw new IllegalStateException("Stripe secret key is not configured");
        }

        if (request == null || request.getProductCode() == null) {
            throw new IllegalArgumentException("Product code is required");
        }

        Product product = productRepository.findByCode(request.getProductCode())
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        String deviceNumber = normalizeDeviceNumber(request.getDeviceNumber());
        if (!validateSerialNum(deviceNumber)) {
            throw new IllegalArgumentException("Device number must be 16 hex characters");
        }

        int subscriptionMonths = OrderService.resolveSubscriptionMonths(product.getCode());
        if (subscriptionMonths > 0) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime maxExpiry = null;
            for (Order paidOrder : orderService.findPaidSubscriptionsByDevice(deviceNumber)) {
                LocalDateTime expiresAt = getOrderExpiresAt(paidOrder);
                if (expiresAt == null) {
                    continue;
                }
                if (maxExpiry == null || expiresAt.isAfter(maxExpiry)) {
                    maxExpiry = expiresAt;
                }
            }
            LocalDateTime repurchaseAllowedAt = now.plusMonths(1);
            if (maxExpiry != null && maxExpiry.isAfter(repurchaseAllowedAt)) {
                String expiresAt = maxExpiry.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                throw new IllegalStateException("An active subscription already exists for this device until " + expiresAt);
            }
        }

        long quantity = request.getQuantity() == null || request.getQuantity() < 1 ? 1 : request.getQuantity();
        if (!"TOKEN".equals(product.getProductType())) {
            quantity = 1;
        }

        Order order = orderService.createPendingOrder(product, (int) quantity, "stripe", userId, username, deviceNumber);

        String form = buildFormBody(product, quantity, order);
        String response = postToStripe(form);
        String checkoutUrl = extractCheckoutUrl(response);
        String stripeSessionId = extractField(response, "id");
        if (checkoutUrl == null || checkoutUrl.trim().isEmpty()) {
            orderService.markFailed(order.getOrderNumber(), "Stripe did not return a checkout URL");
            throw new IllegalStateException("Stripe did not return a checkout URL");
        }
        orderService.attachStripeSession(order.getOrderNumber(), stripeSessionId);
        return new CheckoutResult(checkoutUrl, order.getOrderNumber(), stripeSessionId);
    }

    public List<Product> getProducts() {
        return productRepository.findByActiveTrueOrderBySortOrderAsc();
    }

    public SubscriptionProtocolResponse checkSubscriptionByDevice(String rawDeviceNumber) {
        String deviceNumber = resolveSubscriptionCheckDeviceNumber(rawDeviceNumber);
        Order latestPaidSubscription = findLatestPaidSubscription(deviceNumber);
        if (latestPaidSubscription == null) {
            return SubscriptionProtocolResponse.notFound(deviceNumber);
        }

        LocalDateTime createdAt = latestPaidSubscription.getCreatedAt();
        LocalDateTime expiresAt = getOrderExpiresAt(latestPaidSubscription);
        if (expiresAt == null) {
            return SubscriptionProtocolResponse.notFound(deviceNumber);
        }
        long createdAtUnix = toUnixTime(createdAt);
        long expiresAtUnix = toUnixTime(expiresAt);
        String encryptedStatus = encryptSubscriptionPayload(deviceNumber, createdAtUnix, expiresAtUnix);
        return SubscriptionProtocolResponse.found(deviceNumber, encryptedStatus);
    }

    public Order confirmStripeSession(String stripeSessionId) throws IOException {
        String response = getStripeSession(stripeSessionId);
        String paymentStatus = extractField(response, "payment_status");
        Order order = orderService.findByStripeSessionId(stripeSessionId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found for Stripe session"));
        if ("paid".equalsIgnoreCase(paymentStatus)) {
            boolean newlyPaid = !"PAID".equalsIgnoreCase(order.getStatus());
            Order paidOrder = orderService.markPaidByStripeSession(stripeSessionId, paymentStatus);
            if (newlyPaid) {
                orderConfirmationEmailService.sendOrderConfirmation(paidOrder);
            }
            return paidOrder;
        }
        order.setStripePaymentStatus(paymentStatus);
        return order;
    }

    public Order cancelOrder(String orderNumber, String reason) {
        return orderService.markCancelled(orderNumber, reason);
    }

    public void handleWebhook(String payload, String stripeSignatureHeader) throws Exception {
        if (webhookSecret == null || webhookSecret.trim().isEmpty()) {
            throw new IllegalStateException("Stripe webhook secret is not configured");
        }
        if (stripeSignatureHeader == null || stripeSignatureHeader.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing Stripe-Signature header");
        }

        verifyWebhookSignature(payload, stripeSignatureHeader);

        JsonNode event = objectMapper.readTree(payload);
        String eventType = event.path("type").asText("");
        JsonNode session = event.path("data").path("object");

        if ("checkout.session.completed".equals(eventType)) {
            String sessionId = session.path("id").asText(null);
            String paymentStatus = session.path("payment_status").asText("");
            if (sessionId != null && "paid".equalsIgnoreCase(paymentStatus)) {
                Order order = orderService.findByStripeSessionId(sessionId)
                        .orElseThrow(() -> new IllegalArgumentException("Order not found for Stripe session"));
                boolean newlyPaid = !"PAID".equalsIgnoreCase(order.getStatus());
                Order paidOrder = orderService.markPaidByStripeSession(sessionId, paymentStatus);
                if (newlyPaid) {
                    orderConfirmationEmailService.sendOrderConfirmation(paidOrder);
                }
            }
        }
    }

    private String buildFormBody(Product product, long quantity, Order order) throws IOException {
        StringBuilder body = new StringBuilder();
        appendForm(body, "mode", "payment");
        appendForm(body, "success_url", appendQueryParam(successUrl, "order", order.getOrderNumber()));
        appendForm(body, "cancel_url", appendQueryParam(cancelUrl, "order", order.getOrderNumber()));
        appendForm(body, "payment_method_types[0]", "card");
        appendForm(body, "line_items[0][quantity]", String.valueOf(quantity));
        appendForm(body, "line_items[0][price_data][currency]", "usd");
        appendForm(body, "line_items[0][price_data][product_data][name]", product.getName() + " - " + product.getVariant());
        appendForm(body, "line_items[0][price_data][unit_amount]", String.valueOf(product.getPrice().movePointRight(2).longValue()));
        appendForm(body, "metadata[productCode]", product.getCode());
        appendForm(body, "metadata[quantity]", String.valueOf(quantity));
        appendForm(body, "metadata[deviceNumber]", order.getDeviceNumber());
        appendForm(body, "metadata[orderNumber]", order.getOrderNumber());
        return body.toString();
    }

    private void appendForm(StringBuilder body, String key, String value) throws IOException {
        if (body.length() > 0) {
            body.append('&');
        }
        body.append(URLEncoder.encode(key, "UTF-8"));
        body.append('=');
        body.append(URLEncoder.encode(value, "UTF-8"));
    }

    private String postToStripe(String formBody) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL("https://api.stripe.com/v1/checkout/sessions").openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Authorization", "Bearer " + secretKey);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(formBody.getBytes(StandardCharsets.UTF_8));
        }

        int status = connection.getResponseCode();
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream(),
                StandardCharsets.UTF_8));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }

        if (status < 200 || status >= 300) {
            throw new IllegalStateException("Stripe API error: " + response);
        }

        return response.toString();
    }

    private String getStripeSession(String stripeSessionId) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL("https://api.stripe.com/v1/checkout/sessions/" + stripeSessionId).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", "Bearer " + secretKey);

        int status = connection.getResponseCode();
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream(),
                StandardCharsets.UTF_8));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }

        if (status < 200 || status >= 300) {
            throw new IllegalStateException("Stripe API error: " + response);
        }
        return response.toString();
    }

    private String extractCheckoutUrl(String responseBody) {
        Matcher matcher = Pattern.compile("\"url\"\\s*:\\s*\"([^\"]+)\"").matcher(responseBody);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1).replace("\\/", "/");
    }

    private String extractField(String responseBody, String fieldName) {
        Matcher matcher = Pattern.compile("\"" + fieldName + "\"\\s*:\\s*\"([^\"]+)\"").matcher(responseBody);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1).replace("\\/", "/");
    }

    private String appendQueryParam(String baseUrl, String key, String value) {
        String separator = baseUrl.contains("?") ? "&" : "?";
        return baseUrl + separator + key + "=" + value;
    }

    private String normalizeDeviceNumber(String deviceNumber) {
        if (deviceNumber == null) {
            return "";
        }
        return deviceNumber.trim().replace(" ", "");
    }

    String resolveSubscriptionCheckDeviceNumber(String rawDeviceNumber) {
        String normalizedDeviceNumber = normalizeDeviceNumber(rawDeviceNumber).toUpperCase(Locale.ROOT);
        if (validateSerialNum(normalizedDeviceNumber)) {
            return normalizedDeviceNumber;
        }
        if (normalizedDeviceNumber.length() != PROTOCOL_DEVICE_TOKEN_LEN || !isHex(normalizedDeviceNumber)) {
            throw new IllegalArgumentException("Device number must be 16 hex characters or a 32-hex encrypted token");
        }

        byte[] decryptedToken = decryptAesEcb(hexToBytes(normalizedDeviceNumber), subscriptionCheckRequestAesKey);
        byte[] deviceBytes = new byte[BINARY_SERIAL_LEN];
        byte[] repeatedBytes = new byte[BINARY_SERIAL_LEN];
        System.arraycopy(decryptedToken, 0, deviceBytes, 0, BINARY_SERIAL_LEN);
        System.arraycopy(decryptedToken, BINARY_SERIAL_LEN, repeatedBytes, 0, BINARY_SERIAL_LEN);
        if (!MessageDigest.isEqual(deviceBytes, repeatedBytes)) {
            throw new IllegalArgumentException("Encrypted device token is invalid");
        }
        return bytesToHex(deviceBytes);
    }

    private boolean validateSerialNum(String serial) {
        if (serial.length() != SERIAL_LEN) {
            return false;
        }
        return isHex(serial);
    }

    private void verifyWebhookSignature(String payload, String signatureHeader) throws Exception {
        String[] parts = signatureHeader.split(",");
        String timestamp = null;
        List<String> signatures = new ArrayList<String>();

        for (String part : parts) {
            String[] kv = part.trim().split("=", 2);
            if (kv.length != 2) {
                continue;
            }
            if ("t".equals(kv[0])) {
                timestamp = kv[1];
            } else if ("v1".equals(kv[0])) {
                signatures.add(kv[1]);
            }
        }

        if (timestamp == null || signatures.isEmpty()) {
            throw new IllegalArgumentException("Invalid Stripe-Signature header");
        }

        long timestampValue;
        try {
            timestampValue = Long.parseLong(timestamp);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid Stripe signature timestamp");
        }

        long nowSeconds = System.currentTimeMillis() / 1000;
        if (Math.abs(nowSeconds - timestampValue) > WEBHOOK_TOLERANCE_SECONDS) {
            throw new IllegalArgumentException("Stripe webhook signature has expired");
        }

        String signedPayload = timestamp + "." + payload;
        String expectedSignature = hmacSha256Hex(webhookSecret, signedPayload);

        for (String signature : signatures) {
            if (MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8))) {
                return;
            }
        }

        throw new IllegalArgumentException("Stripe webhook signature verification failed");
    }

    private String hmacSha256Hex(String secret, String payload) throws Exception {
        Mac hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        hmac.init(secretKeySpec);
        byte[] digest = hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

        StringBuilder hex = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    String encryptSubscriptionPayload(String deviceNumber, long createdAtUnix, long expiresAtUnix) {
        if (!validateSerialNum(deviceNumber)) {
            throw new IllegalArgumentException("Device number must be 16 hex characters");
        }

        ByteBuffer payload = ByteBuffer.allocate(16);
        payload.put(hexToBytes(deviceNumber));
        payload.putInt((int) createdAtUnix);
        payload.putInt((int) expiresAtUnix);
        return bytesToHex(encryptAesEcb(payload.array(), subscriptionCheckResponseAesKey));
    }

    private Order findLatestPaidSubscription(String deviceNumber) {
        Order latestPaidSubscription = null;
        LocalDateTime latestExpiry = null;
        for (Order paidOrder : orderService.findPaidSubscriptionsByDevice(deviceNumber)) {
            LocalDateTime expiresAt = getOrderExpiresAt(paidOrder);
            if (expiresAt == null) {
                continue;
            }
            if (latestExpiry == null || expiresAt.isAfter(latestExpiry)) {
                latestExpiry = expiresAt;
                latestPaidSubscription = paidOrder;
            }
        }
        return latestPaidSubscription;
    }

    private LocalDateTime getOrderExpiresAt(Order order) {
        if (order.getExpiresAt() != null) {
            return order.getExpiresAt();
        }
        // fallback for old orders that predate the expires_at column
        if (order.getCreatedAt() == null) {
            return null;
        }
        int months = OrderService.resolveSubscriptionMonths(order.getProductCode());
        return months > 0 ? order.getCreatedAt().plusMonths(months) : null;
    }

    private long toUnixTime(LocalDateTime dateTime) {
        return dateTime.atZone(ZoneId.systemDefault()).toEpochSecond();
    }

    private void validateAesKey(String key, String propertyName) {
        String normalizedKey = normalizeDeviceNumber(key).toUpperCase(Locale.ROOT);
        if (normalizedKey.length() != PROTOCOL_DEVICE_TOKEN_LEN || !isHex(normalizedKey)) {
            throw new IllegalStateException(propertyName + " must be a 32-character hex AES-128 key");
        }
    }

    private boolean isHex(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.digit(value.charAt(i), 16) == -1) {
                return false;
            }
        }
        return true;
    }

    private byte[] encryptAesEcb(byte[] payload, String hexKey) {
        return doAesEcb(payload, hexKey, Cipher.ENCRYPT_MODE);
    }

    private byte[] decryptAesEcb(byte[] payload, String hexKey) {
        return doAesEcb(payload, hexKey, Cipher.DECRYPT_MODE);
    }

    private byte[] doAesEcb(byte[] payload, String hexKey, int cipherMode) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(cipherMode, new SecretKeySpec(hexToBytes(hexKey), "AES"));
            return cipher.doFinal(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to process subscription AES payload", e);
        }
    }

    private byte[] hexToBytes(String value) {
        int length = value.length();
        byte[] bytes = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            int high = Character.digit(value.charAt(i), 16);
            int low = Character.digit(value.charAt(i + 1), 16);
            bytes[i / 2] = (byte) ((high << 4) + low);
        }
        return bytes;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            hex.append(String.format("%02X", b & 0xFF));
        }
        return hex.toString();
    }

    public static class CheckoutResult {
        private final String checkoutUrl;
        private final String orderNumber;
        private final String stripeSessionId;

        public CheckoutResult(String checkoutUrl, String orderNumber, String stripeSessionId) {
            this.checkoutUrl = checkoutUrl;
            this.orderNumber = orderNumber;
            this.stripeSessionId = stripeSessionId;
        }

        public String getCheckoutUrl() {
            return checkoutUrl;
        }

        public String getOrderNumber() {
            return orderNumber;
        }

        public String getStripeSessionId() {
            return stripeSessionId;
        }
    }

    public static class SubscriptionProtocolResponse {
        private final String deviceNumber;
        private final boolean found;
        private final String payload;

        private SubscriptionProtocolResponse(String deviceNumber, boolean found, String payload) {
            this.deviceNumber = deviceNumber;
            this.found = found;
            this.payload = payload;
        }

        public static SubscriptionProtocolResponse found(String deviceNumber, String payload) {
            return new SubscriptionProtocolResponse(deviceNumber, true, payload);
        }

        public static SubscriptionProtocolResponse notFound(String deviceNumber) {
            return new SubscriptionProtocolResponse(deviceNumber, false, EMPTY_SUBSCRIPTION_STATUS);
        }

        public String getDeviceNumber() {
            return deviceNumber;
        }

        public boolean isFound() {
            return found;
        }

        public String getPayload() {
            return payload;
        }
    }
}
