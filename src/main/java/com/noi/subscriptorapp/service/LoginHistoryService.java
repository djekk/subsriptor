package com.noi.subscriptorapp.service;

import com.noi.subscriptorapp.model.LoginHistory;
import com.noi.subscriptorapp.model.User;
import com.noi.subscriptorapp.repository.LoginHistoryRepository;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class LoginHistoryService {
    private final LoginHistoryRepository loginHistoryRepository;

    public LoginHistoryService(LoginHistoryRepository loginHistoryRepository) {
        this.loginHistoryRepository = loginHistoryRepository;
    }

    public void recordLogin(User user, HttpServletRequest request) {
        LoginHistory history = new LoginHistory(
                user.getId(),
                LocalDateTime.now(),
                request.getRemoteAddr(),
                request.getHeader("User-Agent")
        );
        loginHistoryRepository.save(history);
    }

    public void recordLogout(Long userId) {
        if (userId == null) {
            return;
        }

        Optional<LoginHistory> activeLogin = loginHistoryRepository
                .findTopByUserIdAndLogoutTimeIsNullOrderByLoginTimeDesc(userId);

        if (activeLogin.isPresent()) {
            LoginHistory history = activeLogin.get();
            history.setLogoutTime(LocalDateTime.now());
            loginHistoryRepository.save(history);
        }
    }
}
