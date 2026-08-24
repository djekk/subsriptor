package com.noi.subscriptorapp.dto;

public class OrderSummary {
    private String orderNumber;
    private String productCode;
    private String status;
    private String stripeSessionId;
    private String paymentProvider;

    public OrderSummary() {}

    public OrderSummary(String orderNumber, String productCode, String status, String stripeSessionId, String paymentProvider) {
        this.orderNumber = orderNumber;
        this.productCode = productCode;
        this.status = status;
        this.stripeSessionId = stripeSessionId;
        this.paymentProvider = paymentProvider;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStripeSessionId() {
        return stripeSessionId;
    }

    public void setStripeSessionId(String stripeSessionId) {
        this.stripeSessionId = stripeSessionId;
    }

    public String getPaymentProvider() {
        return paymentProvider;
    }

    public void setPaymentProvider(String paymentProvider) {
        this.paymentProvider = paymentProvider;
    }
}
