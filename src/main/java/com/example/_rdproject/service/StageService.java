package com.example._rdproject.service;

import com.example._rdproject.dto.StageProgressDto;
import com.example._rdproject.entity.Stage;
import com.example._rdproject.entity.User;
import com.example._rdproject.entity.UserCharacter;
import com.example._rdproject.entity.Character;
import com.example._rdproject.entity.UserStageProgress;
import com.example._rdproject.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StageService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StageService.class);

    private final UserStageProgressRepository userStageProgressRepository;
    private final StageRepository stageRepository;
    private final UserRepository userRepository;
    private final CharacterRepository characterRepository;
    private final UserCharacterRepository userCharacterRepository;

    /**
     * 스테이지 진행도 완료 처리 및 다음 단계 동적 해금
     */
    @Transactional
    public StageProgressDto.UpdateResponse updateStageProgress(StageProgressDto.UpdateRequest request) {
        log.info("요청 데이터 확인 -> userId: {}, stageId: {}, score: {}, passed: {}",
                request.getUserId(), request.getCurrentStageId(), request.getScore(), request.isPassed());

        // 1. 점수 미달 탈락 시 다음 단계를 열지 않고 즉시 반환
        if (!request.isPassed()) {
            return new StageProgressDto.UpdateResponse(false, false, null);
        }

        // 3. 현재 스테이지 정보 조회를 통해 속한 캐릭터 ID와 순번 파악 (안전한 처리를 위해 위로 이동)
        Stage currentStage = stageRepository.findById(request.getCurrentStageId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 스테이지입니다. ID: " + request.getCurrentStageId()));

        // 2. 현재 스테이지의 진행도 row 조회 및 완료(isCompleted=true) 처리
        Optional<UserStageProgress> currentProgressOpt = userStageProgressRepository
                .findByUserIdAndStageId(request.getUserId(), request.getCurrentStageId());

        UserStageProgress currentProgress;

        if (currentProgressOpt.isPresent()) {
            // 기존 진행 기록 업데이트 (인수가 있어도 이제 오류가 나지 않습니다!)
            currentProgress = currentProgressOpt.get();
            currentProgress.updateScoreAndComplete(request.getScore());
        } else {
            //최초 플레이라 데이터가 없다면 자동으로 새로 파서 INSERT
            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다. ID: " + request.getUserId()));

            currentProgress = UserStageProgress.builder()
                    .user(user)
                    .stage(currentStage)
                    .isUnlocked(true)
                    .isCompleted(true)
                    .bestScore(request.getScore())
                    .build();
            userStageProgressRepository.save(currentProgress);
        }
        System.out.println("DEBUG: 현재 스테이지 정보 -> ID: " + currentStage.getId() +
                ", 캐릭터ID: " + currentStage.getCharacter().getId() +
                ", 스테이지번호: " + currentStage.getStageNumber());

        // 4. 같은 캐릭터의 다음 스테이지(stageNumber + 1)가 존재하는지 레포지토리 조회
        log.info("DEBUG SEARCH: 찾으려는 캐릭터ID = {}, 찾으려는 다음 스테이지 번호 = {}",
                currentStage.getCharacter().getId(),
                currentStage.getStageNumber() + 1);

        Optional<Stage> nextStageOpt = stageRepository.findByCharacterIdAndStageNumber(
                currentStage.getCharacter().getId(),
                currentStage.getStageNumber() + 1
        );
        log.info("DEBUG: 다음 스테이지 검색 결과 존재 여부: {}", nextStageOpt.isPresent());
        System.out.println("DEBUG: 다음 스테이지 검색 결과 존재 여부: " + nextStageOpt.isPresent());

        if (nextStageOpt.isPresent()) {
            System.out.println("DEBUG: 찾아낸 다음 스테이지 ID: " + nextStageOpt.get().getId());
        } else {
            log.warn("DEBUG: 해당 조건(CharacterID: {}, StageNumber: {})으로 스테이지를 찾지 못했습니다! DB 데이터를 다시 확인하세요.",
                    currentStage.getCharacter().getId(),
                    currentStage.getStageNumber() + 1);
            System.out.println("DEBUG: DB 테이블의 character_id와 stage_number를 다시 확인");
        }

        // 5. 다음 스테이지가 존재하면 해금 처리 프로세스 진입
        if (nextStageOpt.isPresent()) {
            Stage nextStage = nextStageOpt.get();

            Optional<UserStageProgress> nextProgressOpt = userStageProgressRepository
                    .findByUserIdAndStageId(request.getUserId(), nextStage.getId());

            if (nextProgressOpt.isPresent()) {
                // 이미 존재한다면 활성화 상태만 해금(true)으로 변경
                nextProgressOpt.get().getIsUnlocked();
            } else {
                // 완전히 처음 도달하는 다음 단계라면 테이블 컬럼 구조에 맞게 연관관계 매핑 객체 생성
                User user = userRepository.findById(request.getUserId())
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다. ID: " + request.getUserId()));

                UserStageProgress newProgress = UserStageProgress.builder()
                        .user(user)
                        .stage(nextStage)
                        .isUnlocked(true)
                        .isCompleted(false)
                        .build();
                userStageProgressRepository.save(newProgress);
            }

            return new StageProgressDto.UpdateResponse(true, true, nextStage.getId());
        }

        // 6. 다음 스테이지가 없는 경우 (해당 캐릭터 엔딩 / 마지막 스테이지 클리어)
        return new StageProgressDto.UpdateResponse(true, false, null);
    }
    /**
     * 특정 캐릭터 획득 처리
     */
    @Transactional
    public void acquireCharacter(Long userId, String characterId) {
        // 1. 유저 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다. ID: " + userId));

        // 2. 캐릭터 조회 (String ID)
        Character character = characterRepository.findById(characterId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 캐릭터입니다. ID: " + characterId));

        // 3. 이미 획득한 캐릭터인지 확인
        boolean alreadyAcquired = userCharacterRepository.existsByUserAndCharacter(user, character);
        if (alreadyAcquired) {
            throw new IllegalStateException("이미 보유 중인 캐릭터입니다.");
        }

        // 4. 캐릭터 획득 처리 (DB 저장)
        UserCharacter userCharacter = UserCharacter.builder()
                .user(user)
                .character(character)
                .affinityScore(0) // 초기 호감도 0
                .isUnlocked(true) // 획득과 동시에 해금
                .build();

        userCharacterRepository.save(userCharacter);
    }
}