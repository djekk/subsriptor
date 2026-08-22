package com.example.loginapp.repository;

import com.example.loginapp.model.LoginHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {
    Optional<LoginHistory> findTopByUserIdAndLogoutTimeIsNullOrderByLoginTimeDesc(Long userId);
}
