package com.noi.subscriptorapp.controller;

import com.noi.subscriptorapp.dto.ApiResponse;
import com.noi.subscriptorapp.dto.StripeCheckoutRequest;
import com.noi.subscriptorapp.dto.StripeCheckoutResponse;
import com.noi.subscriptorapp.model.Order;
import com.noi.subscriptorapp.service.StripePaymentService;
import com.noi.subscriptorapp.service.OrderService;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private static final long SUBSCRIPTION_CHECK_RATE_LIMIT_MS = 5000;
    private static final Map<String, Long> SUBSCRIPTION_CHECK_LAST_HIT = new ConcurrentHashMap<String, Long>();

    private final StripePaymentService stripePaymentService;
    private final OrderService orderService;

    public PaymentController(StripePaymentService stripePaymentService, OrderService orderService) {
        this.stripePaymentService = stripePaymentService;
        this.orderService = orderService;
    }

    @PostMapping("/stripe/checkout")
    public ResponseEntity<?> createStripeCheckout(@RequestBody StripeCheckoutRequest request, HttpSession session) {
        try {
            Long userId = (Long) session.getAttribute("userId");
            String username = (String) session.getAttribute("username");
            StripePaymentService.CheckoutResult result = stripePaymentService.createCheckoutSession(request, userId, username);
            return ResponseEntity.ok(new StripeCheckoutResponse(true, "Checkout session created", result.getCheckoutUrl(), result.getOrderNumber(), result.getStripeSessionId()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Stripe checkout failed: " + e.getMessage()));
        }
    }

    @GetMapping("/stripe/confirm")
    public ResponseEntity<?> confirmStripePayment(@RequestParam("session_id") String sessionId) {
        try {
            Order order = stripePaymentService.confirmStripeSession(sessionId);
            return ResponseEntity.ok(new ApiResponse(true, "Payment confirmed", order));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Unable to confirm payment: " + e.getMessage()));
        }
    }

    @PostMapping("/stripe/webhook")
    public ResponseEntity<?> handleStripeWebhook(
            @RequestHeader(value = "Stripe-Signature", required = false) String stripeSignature,
            @RequestBody(required = false) String payload) {
        try {
            if (payload == null || payload.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ApiResponse(false, "Empty webhook payload"));
            }
            stripePaymentService.handleWebhook(payload, stripeSignature);
            return ResponseEntity.ok(new ApiResponse(true, "Webhook processed"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Webhook processing failed: " + e.getMessage()));
        }
    }

    @GetMapping("/stripe/webhook")
    public ResponseEntity<?> webhookInfo() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(new ApiResponse(false, "Use POST for Stripe webhooks. This endpoint is not for browser GET requests."));
    }

    @GetMapping("/orders/{orderNumber}")
    public ResponseEntity<?> getOrderStatus(@PathVariable String orderNumber) {
        return orderService.findByOrderNumber(orderNumber)
                .<ResponseEntity<?>>map(order -> ResponseEntity.ok(new ApiResponse(true, "Order found", order)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse(false, "Order not found")));
    }

    @GetMapping(value = "/subscriptions/check", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> checkSubscriptionByDevice(
            @RequestParam("deviceNumber") String deviceNumber,
            HttpServletRequest request) {
        try {
            String rateKey = request.getRemoteAddr();
            if (isRateLimited(rateKey)) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .contentType(MediaType.TEXT_PLAIN)
                        .body("error:" + StripePaymentService.EMPTY_SUBSCRIPTION_STATUS);
            }

            StripePaymentService.SubscriptionProtocolResponse result = stripePaymentService.checkSubscriptionByDevice(deviceNumber);
            String responseBody = result.isFound()
                    ? "success:" + result.getPayload()
                    : "error:" + StripePaymentService.EMPTY_SUBSCRIPTION_STATUS;
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(responseBody);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("error:" + StripePaymentService.EMPTY_SUBSCRIPTION_STATUS);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("error:" + StripePaymentService.EMPTY_SUBSCRIPTION_STATUS);
        }
    }

    private boolean isRateLimited(String key) {
        long now = System.currentTimeMillis();
        Long previous = SUBSCRIPTION_CHECK_LAST_HIT.get(key);
        if (previous != null && now - previous < SUBSCRIPTION_CHECK_RATE_LIMIT_MS) {
            return true;
        }
        SUBSCRIPTION_CHECK_LAST_HIT.put(key, now);
        return false;
    }

    @PostMapping("/orders/{orderNumber}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable String orderNumber) {
        try {
            Order order = stripePaymentService.cancelOrder(orderNumber, "User cancelled checkout");
            return ResponseEntity.ok(new ApiResponse(true, "Order cancelled", order));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }
}
