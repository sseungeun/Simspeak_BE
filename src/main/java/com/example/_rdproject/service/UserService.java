package com.example._rdproject.service;

import com.example._rdproject.domain.GenderType;
import com.example._rdproject.dto.GuestAuthDto;
import com.example._rdproject.dto.UserDto;
import com.example._rdproject.entity.User;
import com.example._rdproject.entity.Character;
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
    public GuestAuthDto.GuestLoginResponseData loginOrCreateGuest(GuestAuthDto.GuestLoginRequest request) {
        String guestId = request.getGuest_id();

        // 1. 기존 가입된 게스트 유저가 있는지 조회
        Optional<User> existingUser = userRepository.findByGuestId(guestId);

        if (existingUser.isPresent()) {
            // 2. 기존 유저인 경우 (is_new_user = false)
            User user = existingUser.get();
            return GuestAuthDto.GuestLoginResponseData.builder()
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
                    .preferredPartnerGender(GenderType.female) // 기본값 설정
                    .continuousDays(0)
                    .build();

            User savedUser = userRepository.save(newUser);

            // 4. 닉네임 정식 업데이트
            savedUser.setNickname("Guest_" + savedUser.getId());
            userRepository.save(savedUser); // 변경 감지(Dirty Check) 및 재저장

            return GuestAuthDto.GuestLoginResponseData.builder()
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

    // --- [마이페이지 프로필 수정 비즈니스 로직] ---
    @org.springframework.transaction.annotation.Transactional // DB를 수정할 때는 이 마법의 단어가 꼭 필요합니다!
    public UserDto.UpdateProfileResponse updateProfile(Long userId, UserDto.UpdateProfileRequest request) {
        
        // 1. DB에서 해당 유저 찾기 (없으면 에러 던지기)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저를 찾을 수 없습니다. ID: " + userId));

        // 2. 명세서 규칙 검사: 닉네임은 2자 이상 10자 이하!
        String newNickname = request.getNickname();
        if (newNickname == null || newNickname.length() < 2 || newNickname.length() > 10) {
            throw new IllegalArgumentException("닉네임은 2자 이상 10자 이하로 입력해주세요.");
        }

        // 3. String으로 들어온 성별(female)을 백엔드 전용 Enum 타입(FEMALE)으로 변환
        com.example._rdproject.domain.GenderType newGender = 
            com.example._rdproject.domain.GenderType.valueOf(request.getPreferred_partner_gender().toLowerCase());

        // 4. 아까 Entity에 만들어둔 스위치를 눌러서 정보 업데이트!
        user.updateProfile(newNickname, newGender);

        // 5. 업데이트된 결과를 프론트엔드에게 돌려줄 Response 상자에 담아서 반환
        return UserDto.UpdateProfileResponse.builder()
                .user_id(user.getId())
                .nickname(user.getNickname())
                .preferred_partner_gender(user.getPreferredPartnerGender().name().toLowerCase()) // 다시 소문자(female)로 변환
                .current_level("B1") // 레벨은 임시로 하드코딩 (기존 로직에 맞게 추후 수정 가능)
                .build();
    }

    // --- [마이페이지 프로필 조회 비즈니스 로직] ---
    @org.springframework.transaction.annotation.Transactional(readOnly = true) // '조회'만 할 때는 readOnly=true를 붙이면 속도가 더 빠릅니다!
    public UserDto.GetProfileResponse getProfile(Long userId) {
        
        // 1. DB에서 유저 찾기
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저를 찾을 수 없습니다. ID: " + userId));

        // 2. 성별 정보가 비어있을 수도 있으니 안전하게 꺼내기
        String genderStr = (user.getPreferredPartnerGender() != null) 
                            ? user.getPreferredPartnerGender().name().toLowerCase() 
                            : null;

        // 3. 택배 상자에 담아서 리턴!
        return UserDto.GetProfileResponse.builder()
                .user_id(user.getId())
                .nickname(user.getNickname())
                .preferred_partner_gender(genderStr)
                .current_level("B1") // 레벨은 임시 고정
                .build();
    }
}