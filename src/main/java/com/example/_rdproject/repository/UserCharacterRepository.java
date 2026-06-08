package com.example._rdproject.repository;

import com.example._rdproject.entity.User;
import com.example._rdproject.entity.UserCharacter;
import com.example._rdproject.entity.Character;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserCharacterRepository extends JpaRepository<UserCharacter, Long> {

    // 1. 필요한 메서드 하나만 유지 (이름을 맞춰줍니다)
    @EntityGraph(attributePaths = {"character"})
    List<UserCharacter> findAllByUserId(Long userId);

    // 2. 캐릭터 보유 여부 확인용 메서드 추가
    boolean existsByUserAndCharacter(User user, Character character);
}