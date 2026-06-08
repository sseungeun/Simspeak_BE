package com.example._rdproject.repository;

import com.example._rdproject.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatSessionRepository extends JpaRepository<ChatSession, String> {
}
