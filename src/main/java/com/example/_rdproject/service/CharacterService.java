package com.example._rdproject.service;

import com.example._rdproject.dto.CharacterDto;
import com.example._rdproject.entity.*;
import com.example._rdproject.entity.Character;
import com.example._rdproject.repository.*;
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

    @Transactional
    public CharacterDto.MainStatusResponse getMainStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));

        List<UserCharacter> userCharacters = userCharacterRepository.findAllByUserId(userId);

        if (userCharacters.isEmpty()) {
            List<Character> allCharacters = characterRepository.findAll();
            userCharacters = allCharacters.stream().map(character -> {
                UserCharacter uc = UserCharacter.builder()
                        .user(user)
                        .character(character)
                        .affinityScore(0)
                        .isUnlocked(false)
                        .build();
                return userCharacterRepository.save(uc);
            }).collect(Collectors.toList());
        }

        List<CharacterDto.CharacterStatusResponse> characterStatuses = userCharacters.stream()
                .map(uc -> {
                    Character c = uc.getCharacter();
                    return new CharacterDto.CharacterStatusResponse(
                            c.getId(),
                            c.getName(),
                            c.getDescription(),
                            c.getImageUrl(),
                            uc.getAffinityScore(),
                            uc.getIsUnlocked(),
                            c.getMbti(),
                            c.getStatAffinity() != null ? c.getStatAffinity() : 0, // null 체크 추가
                            c.getStatTsundere() != null ? c.getStatTsundere() : 0, // 수정된 부분
                            c.getStatWit() != null ? c.getStatWit() : 0          // null 체크 추가
                    );
                }).collect(Collectors.toList());
        // 1. 총 대화 횟수 가져오기
        Long totalConversations = chatSessionRepository.countByUserId(userId);

        // 2. 평균 발음 점수 가져오기
        Double avgScore = pronunciationEvaluationRepository.findAverageScoreByUserId(userId);

        // 발음 기록이 아예 없는 신규 유저일 경우 null이 반환되므로 0.0으로 방어 처리
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

                    return CharacterDto.StageResponse.builder()
                            .stageId(stage.getId())
                            .stageNumber(stage.getStageNumber())
                            .stageType(stage.getStageType().name())
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
        List<UserCharacter> myCharacters = userCharacterRepository.findAllByUserId(userId);

        return myCharacters.stream()
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
                            c.getStatTsundere() != null ? c.getStatTsundere() : 0, // null 체크 추가
                            c.getStatWit() != null ? c.getStatWit() : 0          // null 체크 추가
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