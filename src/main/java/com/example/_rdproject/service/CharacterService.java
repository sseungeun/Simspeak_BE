package com.example._rdproject.service;

import com.example._rdproject.dto.CharacterDto;
import com.example._rdproject.entity.Character;
import com.example._rdproject.entity.Stage;
import com.example._rdproject.entity.User;
import com.example._rdproject.entity.UserCharacter;
import com.example._rdproject.entity.UserStageProgress;
import com.example._rdproject.repository.CharacterRepository;
import com.example._rdproject.repository.StageRepository;
import com.example._rdproject.repository.UserCharacterRepository;
import com.example._rdproject.repository.UserRepository;
import com.example._rdproject.repository.UserStageProgressRepository; // 💡 주입 추가
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

    /**
     * 메인 화면 캐릭터 목록 및 유저 상태 종합 조회
     */
    @Transactional
    public CharacterDto.MainStatusResponse getMainStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다. ID: " + userId));

        List<UserCharacter> userCharacters = userCharacterRepository.findAllByUserIdWithCharacter(userId);

        if (userCharacters.isEmpty()) {
            List<Character> allCharacters = characterRepository.findAll();
            userCharacters = allCharacters.stream().map(character -> {
                UserCharacter uc = UserCharacter.builder()
                        .user(user)
                        .character(character)
                        .affinityScore(0)
                        .isUnlocked(character.isInitialUnlocked())
                        .build();
                return userCharacterRepository.save(uc);
            }).collect(Collectors.toList());
        }

        // DTO 반환 시 MBTI 및 3대 성향 스탯 정보 주입
        List<CharacterDto.CharacterStatusResponse> characterStatuses = userCharacters.stream()
                .map(uc -> new CharacterDto.CharacterStatusResponse(
                        uc.getCharacter().getId(),
                        uc.getCharacter().getName(),
                        uc.getCharacter().getDescription(),
                        uc.getCharacter().getImageUrl(),
                        uc.getAffinityScore(),
                        uc.isUnlocked(),
                        uc.getCharacter().getMbti(),
                        uc.getCharacter().getStatAffinity(),
                        uc.getCharacter().getStatTsundere(),
                        uc.getCharacter().getStatWit()
                )).collect(Collectors.toList());

        return new CharacterDto.MainStatusResponse(
                user.getId(),
                user.getNickname(),
                user.getCurrentLevel(),
                user.getContinuousDays(),
                characterStatuses
        );
    }

    /**
     * 특정 캐릭터의 스테이지 목록 + 유저 진척도 종합 조회
     */
    public CharacterDto.StageListResponse getCharacterStages(Long userId, Long characterId) {
        // 1. 캐릭터 기본 정보 검증 및 조회
        Character character = characterRepository.findById(characterId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 캐릭터입니다. ID: " + characterId));

        // 2. 해당 캐릭터에 속한 마스터 데이터 스테이지 목록 정렬 조회
        List<Stage> stages = stageRepository.findByCharacterIdOrderByStageNumberAsc(characterId);

        // 3. 마스터 스테이지 정보에 유저별 진행 스냅샷 데이터(user_stage_progress) 맵핑 연산
        List<CharacterDto.StageResponse> stageResponses = stages.stream()
                .map(stage -> {
                    // 유저의 진행도 레포지토리에서 데이터를 조회
                    Optional<UserStageProgress> progressOpt = userStageProgressRepository
                            .findByUserIdAndStageId(userId, stage.getId());

                    // 테이블에 기록이 있으면 매핑, 완전히 처음 들어온 상태라 기록이 없다면 기본값 세팅
                    boolean isUnlocked = progressOpt.map(UserStageProgress::isUnlocked).orElse(false);
                    boolean isCompleted = progressOpt.map(UserStageProgress::isCompleted).orElse(false);
                    Integer bestScore = progressOpt.map(UserStageProgress::getBestScore).orElse(null);

                    if (stage.getStageNumber() == 1) {
                        isUnlocked = true;
                    }

                    return CharacterDto.StageResponse.builder()
                            .stageId(stage.getId())
                            .stageNumber(stage.getStageNumber())
                            .stageType(stage.getStageType())
                            .title(stage.getTitle())
                            .scenarioId(stage.getScenarioId())
                            .unlockAffinityRatio(stage.getUnlockAffinityRatio())
                            .isUnlocked(isUnlocked)
                            .isCompleted(isCompleted)
                            .bestScore(bestScore)
                            .build();
                }).collect(Collectors.toList());

        // 4. 빌더 패턴을 사용하여 기획 규격 Response 생성
        return CharacterDto.StageListResponse.builder()
                .characterId(character.getId())
                .characterName(character.getName())
                .stages(stageResponses)
                .build();
    }

    /**
     * 현재 유저가 해금/보유한 모든 캐릭터 목록 및 호감도, 성향 스탯 조회
     */
    public List<CharacterDto.MyCharacterResponse> getMyCharacters(Long userId) {
        List<UserCharacter> myCharacters = userCharacterRepository.findAllByUserIdWithCharacter(userId);

        return myCharacters.stream()
                .map(uc -> new CharacterDto.MyCharacterResponse(
                        uc.getCharacter().getId(),
                        uc.getCharacter().getName(),
                        uc.getCharacter().getDescription(),
                        uc.getCharacter().getImageUrl(),
                        uc.getAffinityScore(),
                        uc.getCharacter().getMbti(),
                        uc.getCharacter().getStatAffinity(),
                        uc.getCharacter().getStatTsundere(),
                        uc.getCharacter().getStatWit()
                )).collect(Collectors.toList());
    }
}