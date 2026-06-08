package com.example._rdproject.repository;

import com.example._rdproject.entity.Stage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StageRepository extends JpaRepository<Stage, Long> {

    /**
     * 특정 캐릭터에 속한 모든 스테이지를 번호 순서대로 정렬하여 가져옵니다.
     */
    List<Stage> findByCharacterIdOrderByStageNumberAsc(String characterId);

    /**
     * 특정 캐릭터의 '특정 스테이지 번호'를 가진 단일 스테이지 정보를 조회합니다.
     */
    @Query("SELECT s FROM Stage s WHERE s.character.id = :characterId AND s.stageNumber = :stageNumber")
    Optional<Stage> findByCharacterIdAndStageNumber(@Param("characterId") String characterId, @Param("stageNumber") Integer stageNumber);
}
