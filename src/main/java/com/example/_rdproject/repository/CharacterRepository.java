package com.example._rdproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example._rdproject.entity.Character;

public interface CharacterRepository extends JpaRepository<Character, String> {
}