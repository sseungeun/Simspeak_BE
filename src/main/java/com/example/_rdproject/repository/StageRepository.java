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
    List<Stage> findByCharacterIdOrderByStageNumberAsc(Long characterId);

    /**
     * 특정 캐릭터의 '특정 스테이지 번호'를 가진 단일 스테이지 정보를 조회합니다.
     */
    // 만약 그냥 필드값으로 검색하고 싶다면 아래처럼 명시하는 것이 훨씬 정확합니다.
    @Query("SELECT s FROM Stage s WHERE s.character.id = :characterId AND s.stageNumber = :stageNumber")
    Optional<Stage> findByCharacterIdAndStageNumber(@Param("characterId") Long characterId, @Param("stageNumber") Integer stageNumber);
}
