package com.noi.subscriptorapp.repository;

import com.noi.subscriptorapp.model.LoginHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {
    Optional<LoginHistory> findTopByUserIdAndLogoutTimeIsNullOrderByLoginTimeDesc(Long userId);
}
