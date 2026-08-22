package com.example.loginapp.controller;

import com.example.loginapp.dto.ApiResponse;
import com.example.loginapp.dto.LoginRequest;
import com.example.loginapp.dto.RegisterRequest;
import com.example.loginapp.model.User;
import com.example.loginapp.service.LoginHistoryService;
import com.example.loginapp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {
    @Autowired
    private UserService userService;

    @Autowired
    private LoginHistoryService loginHistoryService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@RequestBody RegisterRequest request) {
        try {
            if (!request.getPassword().equals(request.getConfirmPassword())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ApiResponse(false, "Passwords do not match"));
            }

            User user = userService.registerUser(
                    request.getUsername(),
                    request.getEmail(),
                    request.getPassword(),
                    request.getFirstName(),
                    request.getLastName()
            );

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse(true, "User registered successfully", user));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@RequestBody LoginRequest request, HttpSession session, HttpServletRequest httpRequest) {
        try {
            Optional<User> user = userService.loginUser(request.getUsername(), request.getPassword());

            if (user.isPresent()) {
                session.setAttribute("userId", user.get().getId());
                session.setAttribute("username", user.get().getUsername());
                session.setAttribute("role", user.get().getRole());
                loginHistoryService.recordLogin(user.get(), httpRequest);
                return ResponseEntity.ok(new ApiResponse(true, "Login successful", user.get()));
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse(false, "Invalid username or password"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Login failed: " + e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        loginHistoryService.recordLogout(userId);
        session.invalidate();
        return ResponseEntity.ok(new ApiResponse(true, "Logout successful"));
    }

    @GetMapping("/check-session")
    public ResponseEntity<ApiResponse> checkSession(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        String username = (String) session.getAttribute("username");
        String role = (String) session.getAttribute("role");

        if (userId != null && username != null && role != null) {
            Map<String, Object> sessionData = new HashMap<String, Object>();
            sessionData.put("userId", userId);
            sessionData.put("username", username);
            sessionData.put("role", role);
            return ResponseEntity.ok(new ApiResponse(true, "Session active", sessionData));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse(false, "No active session"));
    }
}
