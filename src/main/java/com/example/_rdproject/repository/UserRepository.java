package com.example._rdproject.repository;

import com.example._rdproject.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // guestId로 기존 유저가 있는지 조회하는 쿼리
    Optional<User> findByGuestId(String guestId);
}