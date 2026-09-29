package com.noi.subscriptorapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class SubcriptorApplication extends SpringBootServletInitializer {
    public static void main(String[] args) {
        SpringApplication.run(SubcriptorApplication.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(SubcriptorApplication.class);
    }
}
