package com.example.loginapp.dto;

public class StripeCheckoutResponse {
    private boolean success;
    private String message;
    private String checkoutUrl;
    private String orderNumber;
    private String stripeSessionId;

    public StripeCheckoutResponse() {}

    public StripeCheckoutResponse(boolean success, String message, String checkoutUrl) {
        this.success = success;
        this.message = message;
        this.checkoutUrl = checkoutUrl;
    }

    public StripeCheckoutResponse(boolean success, String message, String checkoutUrl, String orderNumber, String stripeSessionId) {
        this.success = success;
        this.message = message;
        this.checkoutUrl = checkoutUrl;
        this.orderNumber = orderNumber;
        this.stripeSessionId = stripeSessionId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCheckoutUrl() {
        return checkoutUrl;
    }

    public void setCheckoutUrl(String checkoutUrl) {
        this.checkoutUrl = checkoutUrl;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getStripeSessionId() {
        return stripeSessionId;
    }

    public void setStripeSessionId(String stripeSessionId) {
        this.stripeSessionId = stripeSessionId;
    }
}
