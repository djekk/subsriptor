package com.noi.subscriptorapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class PageController {
    @GetMapping("/")
    public String index() {
        return "index.html";
    }

    @GetMapping("/login")
    public String login() {
        return "login.html";
    }

    @GetMapping("/register")
    public String register() {
        return "register.html";
    }

    @GetMapping("/verify-email")
    public String verifyEmail() {
        return "verify-email.html";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard.html";
    }

    @GetMapping("/admin")
    public String admin() {
        return "admin.html";
    }

    @GetMapping("/payment")
    public String payment() {
        return "payment.html";
    }

    @GetMapping("/payment/success")
    public ModelAndView paymentSuccess() {
        return new ModelAndView("forward:/payment-success.html");
    }

    @GetMapping("/payment/cancel")
    public String paymentCancel() {
        return "payment-cancel.html";
    }
}
