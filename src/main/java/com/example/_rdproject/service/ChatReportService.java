package com.example._rdproject.service;

import com.example._rdproject.dto.*;
import com.example._rdproject.entity.*;
import com.example._rdproject.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatReportService {

    private final ChatLogRepository chatLogRepository;
    private final CorrectionRepository correctionRepository;
    private final UserRepository userRepository;
    private final RawAiResponseRepository rawAiResponseRepository;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final LocalFileService fileService;
    private final ChatSessionRepository chatSessionRepository;

    public ReportDto.SessionReportResponse getSessionReport(String sessionId, Long userId) {
        List<Correction> corrections = correctionRepository.findByChatLog_SessionId(sessionId);

        List<ReportDto.CorrectionItem> items = corrections.stream()
                .map(c -> {
                    // JSON 파싱 로직
                    String feedback = null;
                    try {
                        if (c.getCorrectionsJson() != null) {
                            // DB에 저장된 JSON(String)을 Map으로 변환
                            Map<String, Object> map = c.getCorrectionsJson();
                            feedback = (String) map.get("grammar_feedback"); // JSON 내 키값에 맞춰 수정
                        }
                    } catch (Exception e) {
                        feedback = "피드백을 불러올 수 없습니다.";
                    }

                    return ReportDto.CorrectionItem.builder()
                            .correction_id(c.getId())
                            .original_sentence(c.getOriginalSentence())
                            .corrected_sentence(c.getCorrectedSentence())
                            .corrected_audio_url(c.getCorrectedAudioUrl())
                            .grammar_feedback(feedback) // ◀ 여기서 추출한 feedback을 넣어줍니다
                            .build();
                })
                .collect(Collectors.toList());

        return ReportDto.SessionReportResponse.builder()
                .session_id(sessionId)
                .corrections(items)
                .build();
    }
    // 2. 오답 노트 업데이트
    @Transactional
    public CorrectionDto.CorrectionUpdateResponse updateCorrection(Long correctionId, CorrectionDto.CorrectionUpdateRequest req) {
        Correction correction = correctionRepository.findById(correctionId)
                .orElseThrow(() -> new IllegalArgumentException("오답 노트를 찾을 수 없습니다."));

        correction.setTranslation(req.getTranslation());
        correction.setIsReviewed(req.getIs_reviewed());

        return CorrectionDto.CorrectionUpdateResponse.builder()
                .correction_id(correction.getId())
                .is_reviewed(correction.getIsReviewed())
                .updated_at(Instant.now())
                .build();
    }

    // 3. 전체 미복습/복습 목록 조회
    public List<CorrectionDto.CorrectionSummary> getCorrections(Long userId, Boolean isReviewed) {
        List<Correction> list;
        if (isReviewed == null) {
            list = correctionRepository.findByChatLog_User_Id(userId);
        } else {
            list = correctionRepository.findByChatLog_User_IdAndIsReviewed(userId, isReviewed);
        }

        return list.stream()
                .map(c -> CorrectionDto.CorrectionSummary.builder()
                        .correction_id(c.getId())
                        .original_sentence(c.getOriginalSentence())
                        .corrected_sentence(c.getCorrectedSentence())
                        .corrected_audio_url(c.getCorrectedAudioUrl())
                        .build())
                .collect(Collectors.toList());
    }

    // 4. AI 분석 및 저장
    @Transactional
    public AiResponseDto analyzeAndSave(AiRequestDto request, MultipartFile audioFile) {
        // 1. User 조회
        User user = userRepository.findById(Long.valueOf(request.getUserId()))
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));

        // 2. 로컬 파일 업로드
        String audioUrl = fileService.upload(audioFile);

        // 3. ChatLog 저장
        ChatLog chatLog = ChatLog.builder()
                .user(user)
                .userText(request.getText())
                .userAudioUrl(audioUrl)

                .build();
        chatLogRepository.save(chatLog);

        // 4. AI 서버 호출
        AiResponseDto aiResult = webClient.post()
                .uri("/analyze")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AiResponseDto.class)
                .block();

        if (aiResult == null) throw new RuntimeException("AI 분석 실패");

        // 5. RawAiResponse 저장
        Map<String, Object> responseMap = objectMapper.convertValue(aiResult, Map.class);
        rawAiResponseRepository.save(RawAiResponse.builder()
                .chatLog(chatLog)
                .responseData(responseMap)
                .createdAt(Instant.now())
                .build());

        // 6. Correction 저장
        if (aiResult.getSystemEvaluation() != null && aiResult.getSystemEvaluation().getCorrectionsJson() != null) {
            for (AiResponseDto.CorrectionItem item : aiResult.getSystemEvaluation().getCorrectionsJson()) {
                correctionRepository.save(Correction.builder()
                        .chatLog(chatLog)
                        .originalSentence(item.getOriginalSentence()) // 카멜 케이스로 수정
                        .correctedSentence(item.getCorrectedSentence()) // 카멜 케이스로 수정
                        .correctedAudioUrl(item.getCorrectedAudioUrl()) // 카멜 케이스로 수정
                        .isReviewed(false)
                        .build());
            }
        }
        return aiResult;
    }

    // 4. 캐릭터별 세션(학습 기록) 목록 조회
    public List<ReportDto.SessionSummaryResponse> getCharacterSessions(String characterId, Long userId) {
        // chat_logs 기반이 아니라 chat_sessions 기반 정방향 쿼리 조회로 변경
        List<ChatSession> sessions = chatSessionRepository.findByCharacterIdAndUserId(characterId, userId);

        return java.util.stream.IntStream.range(0, sessions.size())
                .mapToObj(index -> {
                    ChatSession s = sessions.get(index);
                    return ReportDto.SessionSummaryResponse.builder()
                            .sessionId(s.getSessionId())
                            .dayNumber(index + 1)
                            .latestDay(sessions.size())
                            .build();
                })
                .collect(Collectors.toList());
    }

    // 5. 세션 종료 처리 (엔딩)
    @Transactional
    public ReportDto.SessionEndResponse endSession(String sessionId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 학습 세션방 식별자입니다: " + sessionId));

        // 종료 상태 true로 강제 전환 더티체킹 영속화 이행
        session.setIsCompleted(true);
        chatSessionRepository.save(session);

        return ReportDto.SessionEndResponse.builder()
                .sessionId(sessionId)
                .finalAffinity(85)
                .achievedLevel("B2")
                .build();
    }
}