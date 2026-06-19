package com.example._rdproject.service;

import com.example._rdproject.dto.CharacterDto;
import com.example._rdproject.entity.*;
import com.example._rdproject.entity.Character;
import com.example._rdproject.repository.*;
import com.example._rdproject.domain.GenderType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CharacterService {

    private final UserRepository userRepository;
    private final UserCharacterRepository userCharacterRepository;
    private final StageRepository stageRepository;
    private final CharacterRepository characterRepository;
    private final UserStageProgressRepository userStageProgressRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final PronunciationEvaluationRepository pronunciationEvaluationRepository;

    private boolean isCharacterMatchesGender(String characterId, GenderType preferredGender) {
        if (preferredGender == GenderType.female) {
            return "sienna".equalsIgnoreCase(characterId) || "chloe".equalsIgnoreCase(characterId) || "yoon".equalsIgnoreCase(characterId);
        } else {
            return "liam".equalsIgnoreCase(characterId) || "ian".equalsIgnoreCase(characterId) || "june".equalsIgnoreCase(characterId);
        }
    }

    private boolean isCharacterAllStagesCompleted(Long userId, String characterId) {
        List<Stage> stages = stageRepository.findByCharacterIdOrderByStageNumberAsc(characterId);
        if (stages.isEmpty()) {
            return false;
        }
        for (Stage stage : stages) {
            Optional<UserStageProgress> progressOpt = userStageProgressRepository.findByUserIdAndStageId(userId, stage.getId());
            if (progressOpt.isEmpty() || !Boolean.TRUE.equals(progressOpt.get().getIsCompleted())) {
                return false;
            }
        }
        return true;
    }

    @Transactional
    public CharacterDto.MainStatusResponse getMainStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));

        GenderType preferredGender = user.getPreferredPartnerGender();

        // 캐릭터별 올클리어 여부 실시간 확인
        boolean isSiennaCleared = isCharacterAllStagesCompleted(userId, "sienna");
        boolean isLiamCleared = isCharacterAllStagesCompleted(userId, "liam");
        boolean isChloeCleared = isCharacterAllStagesCompleted(userId, "chloe");
        boolean isIanCleared = isCharacterAllStagesCompleted(userId, "ian");

        List<UserCharacter> userCharacters = userCharacterRepository.findAllByUserId(userId);

        if (userCharacters.isEmpty()) {
            List<Character> allCharacters = characterRepository.findAll();
            userCharacters = allCharacters.stream().map(character -> {
                boolean isDefaultUnlocked = "sienna".equalsIgnoreCase(character.getId()) || "liam".equalsIgnoreCase(character.getId());

                UserCharacter uc = UserCharacter.builder()
                        .user(user)
                        .character(character)
                        .affinityScore(0)
                        .isUnlocked(isDefaultUnlocked)
                        .build();
                return userCharacterRepository.save(uc);
            }).collect(Collectors.toList());
        }

        List<CharacterDto.CharacterStatusResponse> characterStatuses = userCharacters.stream()
                .filter(uc -> isCharacterMatchesGender(uc.getCharacter().getId(), preferredGender))
                .map(uc -> {
                    Character c = uc.getCharacter();
                    String cid = c.getId();

                    // 계단식 해금 조건 적용
                    boolean unlocked = false;
                    if (cid.equalsIgnoreCase("sienna") || cid.equalsIgnoreCase("liam")) {
                        unlocked = true;
                    } else if (cid.equalsIgnoreCase("chloe")) {
                        unlocked = isSiennaCleared;
                    } else if (cid.equalsIgnoreCase("ian")) {
                        unlocked = isLiamCleared;
                    } else if (cid.equalsIgnoreCase("yoon")) {
                        unlocked = isChloeCleared;
                    } else if (cid.equalsIgnoreCase("june")) {
                        unlocked = isIanCleared;
                    }

                    if (uc.getIsUnlocked() != unlocked) {
                        uc.setIsUnlocked(unlocked);
                        userCharacterRepository.save(uc);
                    }

                    return new CharacterDto.CharacterStatusResponse(
                            c.getId(),
                            c.getName(),
                            c.getDescription(),
                            c.getImageUrl(),
                            uc.getAffinityScore(),
                            unlocked,
                            c.getMbti(),
                            c.getStatAffinity() != null ? c.getStatAffinity() : 0,
                            c.getStatTsundere() != null ? c.getStatTsundere() : 0,
                            c.getStatWit() != null ? c.getStatWit() : 0
                    );
                }).collect(Collectors.toList());

        Long totalConversations = chatSessionRepository.countByUserId(userId);
        Double avgScore = pronunciationEvaluationRepository.findAverageScoreByUserId(userId);

        if (avgScore == null) {
            avgScore = 0.0;
        } else {
            avgScore = Math.round(avgScore * 10) / 10.0;
        }

        return new CharacterDto.MainStatusResponse(
                user.getId(),
                user.getNickname(),
                user.getCurrentLevel(),
                user.getContinuousDays(),
                characterStatuses,
                totalConversations,
                avgScore
        );
    }

    // CharacterService.java 내부의 getCharacterStages 메서드를 아래 코드로 통째로 갈아 끼워주세요.
    public CharacterDto.StageListResponse getCharacterStages(Long userId, String characterId) {
        Character character = characterRepository.findById(characterId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 캐릭터입니다."));

        List<Stage> stages = stageRepository.findByCharacterIdOrderByStageNumberAsc(characterId);

        List<CharacterDto.StageResponse> stageResponses = stages.stream()
                .map(stage -> {
                    Optional<UserStageProgress> progressOpt = userStageProgressRepository
                            .findByUserIdAndStageId(userId, stage.getId());

                    boolean isUnlocked = progressOpt.map(UserStageProgress::getIsUnlocked).orElse(false);
                    boolean isCompleted = progressOpt.map(UserStageProgress::getIsCompleted).orElse(false);
                    Integer bestScore = progressOpt.map(UserStageProgress::getBestScore).orElse(null);

                    if (stage.getStageNumber() == 1) isUnlocked = true;

                    // ───────── [6번 버그 해결 핵심 레이어] Null 가드 장착 ─────────
                    String stageTypeName = "regular"; // 안정적인 기본값 폴백 셋업
                    if (stage.getStageType() != null) {
                        stageTypeName = stage.getStageType().name();
                    }

                    return CharacterDto.StageResponse.builder()
                            .stageId(stage.getId())
                            .stageNumber(stage.getStageNumber())
                            .stageType(stageTypeName) // ◀ 가드 가공 완료된 폴백 문자열 주입
                            .title(stage.getTitle())
                            .scenarioId(stage.getScenarioId())
                            .unlockAffinityRatio(stage.getUnlockAffinityRatio())
                            .isUnlocked(isUnlocked)
                            .isCompleted(isCompleted)
                            .bestScore(bestScore)
                            .build();
                }).collect(Collectors.toList());

        return CharacterDto.StageListResponse.builder()
                .characterId(character.getId())
                .characterName(character.getName())
                .stages(stageResponses)
                .build();
    }

    public List<CharacterDto.MyCharacterResponse> getMyCharacters(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));

        GenderType preferredGender = user.getPreferredPartnerGender();

        boolean isSiennaCleared = isCharacterAllStagesCompleted(userId, "sienna");
        boolean isLiamCleared = isCharacterAllStagesCompleted(userId, "liam");
        boolean isChloeCleared = isCharacterAllStagesCompleted(userId, "chloe");
        boolean isIanCleared = isCharacterAllStagesCompleted(userId, "ian");

        List<UserCharacter> myCharacters = userCharacterRepository.findAllByUserId(userId);

        return myCharacters.stream()
                .filter(uc -> isCharacterMatchesGender(uc.getCharacter().getId(), preferredGender))
                .filter(uc -> {
                    String cid = uc.getCharacter().getId();
                    if (cid.equalsIgnoreCase("sienna") || cid.equalsIgnoreCase("liam")) {
                        return true;
                    } else if (cid.equalsIgnoreCase("chloe")) {
                        return isSiennaCleared;
                    } else if (cid.equalsIgnoreCase("ian")) {
                        return isLiamCleared;
                    } else if (cid.equalsIgnoreCase("yoon")) {
                        return isChloeCleared;
                    } else if (cid.equalsIgnoreCase("june")) {
                        return isIanCleared;
                    }
                    return false;
                })
                .map(uc -> {
                    Character c = uc.getCharacter();
                    return new CharacterDto.MyCharacterResponse(
                            c.getId(),
                            c.getName(),
                            c.getDescription(),
                            c.getImageUrl(),
                            uc.getAffinityScore(),
                            c.getMbti(),
                            c.getStatAffinity() != null ? c.getStatAffinity() : 0,
                            c.getStatTsundere() != null ? c.getStatTsundere() : 0,
                            c.getStatWit() != null ? c.getStatWit() : 0
                    );
                }).collect(Collectors.toList());
    }

    @Transactional
    public void acquireCharacter(Long userId, String characterId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다: " + userId));

        Character character = characterRepository.findById(characterId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 캐릭터입니다: " + characterId));

        if (userCharacterRepository.existsByUserAndCharacter(user, character)) {
            throw new IllegalStateException("이미 보유한 캐릭터입니다.");
        }

        UserCharacter newAcquisition = UserCharacter.builder()
                .user(user)
                .character(character)
                .affinityScore(0)
                .isUnlocked(true)
                .build();

        userCharacterRepository.save(newAcquisition);
    }
}