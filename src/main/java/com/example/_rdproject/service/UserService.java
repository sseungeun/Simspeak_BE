package com.example._rdproject.service;

import com.example._rdproject.dto.GuestAuthDto;
import com.example._rdproject.entity.Character;
import com.example._rdproject.entity.User;
import com.example._rdproject.repository.CharacterRepository;
import com.example._rdproject.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final CharacterRepository characterRepository;

    @Transactional
    public GuestAuthDto.ResponseData loginOrCreateGuest(GuestAuthDto.Request request) {
        String guestId = request.getGuest_id();

        // 1. 기존 가입된 게스트 유저가 있는지 조회
        Optional<User> existingUser = userRepository.findByGuestId(guestId);

        if (existingUser.isPresent()) {
            // 2. 기존 유저인 경우 (is_new_user = false)
            User user = existingUser.get();
            return GuestAuthDto.ResponseData.builder()
                    .user_id(user.getId())
                    .nickname(user.getNickname())
                    .current_level(user.getCurrentLevel() != null ? user.getCurrentLevel().name() : null)
                    .continuous_days(user.getContinuousDays())
                    .is_new_user(false)
                    .build();
        } else {
            // 3. 신규 유저인 경우 (임시 닉네임으로 선저장 후 ID 획득)
            User newUser = User.builder()
                    .guestId(guestId)
                    .nickname("TemporaryGuest") // ID 발급 전에 임시 세팅
                    .preferredPartnerGender(User.Gender.female) // 기본값 설정
                    .continuousDays(0)
                    .build();

            User savedUser = userRepository.save(newUser);

            // 4. 닉네임 정식 업데이트
            savedUser.setNickname("Guest_" + savedUser.getId());
            userRepository.save(savedUser); // 변경 감지(Dirty Check) 및 재저장

            return GuestAuthDto.ResponseData.builder()
                    .user_id(savedUser.getId())
                    .nickname(savedUser.getNickname())
                    .current_level(null) // 신규 가입 null 처리
                    .continuous_days(savedUser.getContinuousDays())
                    .is_new_user(true)
                    .build();
        }
    }
    /**
     * 메인 로비/대화 진입 시 최근 사용 캐릭터 ID 업데이트
     */
    public void updateLastCharacter(Long userId, String characterId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다. ID: " + userId));

        Character character = characterRepository.findById(characterId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 캐릭터입니다. ID: " + characterId));
        user.updateLastCharacter(character);
    }
}