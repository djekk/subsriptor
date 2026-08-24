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
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class StripePaymentService {
    private static final long WEBHOOK_TOLERANCE_SECONDS = 300;
    private static final int SERIAL_LEN = 16;

    private final ProductRepository productRepository;
    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    @Value("${stripe.secret-key:}")
    private String secretKey;

    @Value("${stripe.webhook-secret:}")
    private String webhookSecret;

    @Value("${stripe.success-url}")
    private String successUrl;

    @Value("${stripe.cancel-url}")
    private String cancelUrl;

    public StripePaymentService(ProductRepository productRepository, OrderService orderService, ObjectMapper objectMapper) {
        this.productRepository = productRepository;
        this.orderService = orderService;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
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

        int subscriptionMonths = resolveSubscriptionMonths(product.getCode());
        if (subscriptionMonths > 0) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime maxExpiry = null;
            for (Order paidOrder : orderService.findPaidSubscriptionsByDevice(deviceNumber)) {
                int paidMonths = resolveSubscriptionMonths(paidOrder.getProductCode());
                if (paidMonths <= 0 || paidOrder.getCreatedAt() == null) {
                    continue;
                }
                LocalDateTime expiresAt = paidOrder.getCreatedAt().plusMonths(paidMonths);
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

    public SubscriptionCheckResult checkSubscriptionByDevice(String rawDeviceNumber) {
        String deviceNumber = normalizeDeviceNumber(rawDeviceNumber);
        if (!validateSerialNum(deviceNumber)) {
            throw new IllegalArgumentException("Device number must be 16 hex characters");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime maxExpiry = null;
        for (Order paidOrder : orderService.findPaidSubscriptionsByDevice(deviceNumber)) {
            int paidMonths = resolveSubscriptionMonths(paidOrder.getProductCode());
            if (paidMonths <= 0 || paidOrder.getCreatedAt() == null) {
                continue;
            }
            LocalDateTime expiresAt = paidOrder.getCreatedAt().plusMonths(paidMonths);
            if (maxExpiry == null || expiresAt.isAfter(maxExpiry)) {
                maxExpiry = expiresAt;
            }
        }

        boolean active = maxExpiry != null && maxExpiry.isAfter(now);
        boolean canBuyNow = maxExpiry == null || !maxExpiry.isAfter(now.plusMonths(1));
        Long daysUntilExpiry = maxExpiry == null ? null : ChronoUnit.DAYS.between(now, maxExpiry);
        String expiresAt = maxExpiry == null ? null : maxExpiry.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String canBuyAt = maxExpiry == null
                ? now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                : maxExpiry.minusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        return new SubscriptionCheckResult(deviceNumber, active, canBuyNow, expiresAt, canBuyAt, daysUntilExpiry);
    }

    public Order confirmStripeSession(String stripeSessionId) throws IOException {
        String response = getStripeSession(stripeSessionId);
        String paymentStatus = extractField(response, "payment_status");
        Order order = orderService.findByStripeSessionId(stripeSessionId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found for Stripe session"));
        if ("paid".equalsIgnoreCase(paymentStatus)) {
            return orderService.markPaidByStripeSession(stripeSessionId, paymentStatus);
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
                orderService.markPaidByStripeSession(sessionId, paymentStatus);
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

    private boolean validateSerialNum(String serial) {
        if (serial.length() != SERIAL_LEN) {
            return false;
        }
        for (int i = 0; i < SERIAL_LEN; i++) {
            if (Character.digit(serial.charAt(i), 16) == -1) {
                return false;
            }
        }
        return true;
    }

    private int resolveSubscriptionMonths(String productCode) {
        if ("SUBSCRIPTION_6M".equalsIgnoreCase(productCode)) {
            return 6;
        }
        if ("SUBSCRIPTION_1Y".equalsIgnoreCase(productCode)) {
            return 12;
        }
        return 0;
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

    public static class SubscriptionCheckResult {
        private final String deviceNumber;
        private final boolean active;
        private final boolean canBuyNow;
        private final String expiresAt;
        private final String canBuyAt;
        private final Long daysUntilExpiry;

        public SubscriptionCheckResult(String deviceNumber, boolean active, boolean canBuyNow, String expiresAt, String canBuyAt, Long daysUntilExpiry) {
            this.deviceNumber = deviceNumber;
            this.active = active;
            this.canBuyNow = canBuyNow;
            this.expiresAt = expiresAt;
            this.canBuyAt = canBuyAt;
            this.daysUntilExpiry = daysUntilExpiry;
        }

        public String getDeviceNumber() {
            return deviceNumber;
        }

        public boolean isActive() {
            return active;
        }

        public boolean isCanBuyNow() {
            return canBuyNow;
        }

        public String getExpiresAt() {
            return expiresAt;
        }

        public String getCanBuyAt() {
            return canBuyAt;
        }

        public Long getDaysUntilExpiry() {
            return daysUntilExpiry;
        }
    }
}
