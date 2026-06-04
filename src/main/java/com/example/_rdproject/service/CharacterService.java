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
    private final UserCharacterRepository userCharacterRepository; // DB 테이블: user_characters 맵핑
    private final StageRepository stageRepository;
    private final CharacterRepository characterRepository; // DB 테이블: characters 맵핑
    private final UserStageProgressRepository userStageProgressRepository;

    /**
     * 메인 화면 캐릭터 목록 및 유저 상태 종합 조회
     */
    @Transactional
    public CharacterDto.MainStatusResponse getMainStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다. ID: " + userId));

        List<UserCharacter> userCharacters = userCharacterRepository.findAllByUserId(userId);

        if (userCharacters.isEmpty()) {
            List<Character> allCharacters = characterRepository.findAll();
            userCharacters = allCharacters.stream().map(character -> {
                UserCharacter uc = UserCharacter.builder()
                        .user(user)
                        .character(character) // 연관관계 편의 혹은 내부적으로 character_id(String) 저장
                        .affinityScore(0)
                        .isUnlocked(character.isInitialUnlocked())
                        .build();
                return userCharacterRepository.save(uc);
            }).collect(Collectors.toList());
        }

        // DTO 반환 시 MBTI 및 3대 성향 스탯 정보 주입
        List<CharacterDto.CharacterStatusResponse> characterStatuses = userCharacters.stream()
                .map(uc -> new CharacterDto.CharacterStatusResponse(
                        uc.getCharacter().getId(), // 이제 String ('CH_LEO_01' 등)이 반환됨
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
    public CharacterDto.StageListResponse getCharacterStages(Long userId, String characterId) {
        // 1. 캐릭터 기본 정보 검증 및 조회 (String ID 기반)
        Character character = characterRepository.findById(characterId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 캐릭터입니다. ID: " + characterId));

        // 2. 해당 캐릭터에 속한 마스터 데이터 스테이지 목록 정렬 조회 (String characterId 기반)
        List<Stage> stages = stageRepository.findByCharacterIdOrderByStageNumberAsc(characterId);

        // 3. 마스터 스테이지 정보에 유저별 진행 스냅샷 데이터(user_stage_progress) 맵핑 연산
        List<CharacterDto.StageResponse> stageResponses = stages.stream()
                .map(stage -> {
                    // 유저의 진행도 레포지토리에서 데이터를 조회 (stage.getId()는 BIGINT이므로 Long 그대로 사용)
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
                .characterId(character.getId()) // String ID 매핑
                .characterName(character.getName())
                .stages(stageResponses)
                .build();
    }

    /**
     * 현재 유저가 해금/보유한 모든 캐릭터 목록 및 호감도, 성향 스탯 조회
     */
    public List<CharacterDto.MyCharacterResponse> getMyCharacters(Long userId) {
        List<UserCharacter> myCharacters = userCharacterRepository.findAllByUserId(userId);

        return myCharacters.stream()
                .map(uc -> new CharacterDto.MyCharacterResponse(
                        uc.getCharacter().getId(), // String ID 매핑
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
    /**
     * 캐릭터 획득/해금 처리
     */
    @Transactional
    public void acquireCharacter(Long userId, String characterId) {
        // 1. 유저 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));

        // 2. 캐릭터 조회
        Character character = characterRepository.findById(characterId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 캐릭터입니다."));

        // 3. 이미 보유 중인지 확인
        if (userCharacterRepository.existsByUserAndCharacter(user, character)) {
            throw new IllegalStateException("이미 보유한 캐릭터입니다.");
        }

        // 4. 보유 테이블(UserCharacter)에 추가
        UserCharacter newAcquisition = UserCharacter.builder()
                .user(user)
                .character(character)
                .affinityScore(0)
                .isUnlocked(true) // 획득 시 바로 해금
                .build();

        userCharacterRepository.save(newAcquisition);
    }
}