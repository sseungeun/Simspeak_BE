package com.example._rdproject.repository;

import com.example._rdproject.entity.UserCharacterStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserCharacterStatusRepository extends JpaRepository<UserCharacterStatus, Long> {

    // 특정 유저가 특정 캐릭터를 이미 가지고 있는지 확인하는 쿼리 메서드
    boolean existsByUserIdAndCharacterId(Long userId, String characterId);

    Optional<UserCharacterStatus> findByUserIdAndCharacterId(Long userId, String characterId);
}