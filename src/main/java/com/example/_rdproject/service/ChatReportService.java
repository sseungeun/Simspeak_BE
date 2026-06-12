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
    private final PronunciationEvaluationRepository evaluationRepository;
    private final UserRepository userRepository;
    private final RawAiResponseRepository rawAiResponseRepository;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final LocalFileService fileService;

    // 1. 세션 리포트 조회
    public ReportDto.SessionReportResponse getSessionReport(String sessionId, Long userId) {
        List<Correction> corrections = correctionRepository.findByChatLog_SessionId(sessionId);

        List<ReportDto.CorrectionItem> items = corrections.stream()
                .map(c -> ReportDto.CorrectionItem.builder()
                        .correction_id(c.getId())
                        .original_sentence(c.getOriginalSentence())
                        .corrected_sentence(c.getCorrectedSentence())
                        .corrected_audio_url(c.getCorrectedAudioUrl()) // 추가된 필드 반영
                        .grammar_feedback(c.getChatLog().getGrammarFeedback())
                        .build())
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
                .textContent(request.getText())
                .audioUrl(audioUrl)
                .actionDescription(request.getActionDescription())
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
        if (aiResult.getSystemEvaluation() != null && aiResult.getSystemEvaluation().getCorrections_json() != null) {
            for (AiResponseDto.CorrectionItem item : aiResult.getSystemEvaluation().getCorrections_json()) {
                correctionRepository.save(Correction.builder()
                        .chatLog(chatLog)
                        .originalSentence(item.getOriginal_sentence())
                        .correctedSentence(item.getCorrected_sentence())
                        .correctedAudioUrl(item.getCorrected_audio_url())
                        .isReviewed(false)
                        .build());
            }
        }
        return aiResult;
    }

    // 4. 캐릭터별 세션(학습 기록) 목록 조회
    public List<ReportDto.SessionSummaryResponse> getCharacterSessions(String characterId, Long userId) {
        List<ChatLog> logs = chatLogRepository.findByCharacterIdAndUserId(characterId, userId);

        // 세션별로 그룹화하여 데이터 가공
        Map<String, List<ChatLog>> groupedLogs = logs.stream()
                .collect(Collectors.groupingBy(ChatLog::getSessionId));

        int totalSessions = groupedLogs.size();

        return groupedLogs.entrySet().stream()
                .map(entry -> ReportDto.SessionSummaryResponse.builder()
                        .session_id(entry.getKey())
                        .day_number(1) // 필요 시 세션 생성 순서에 따라 번호 부여
                        .latest_day(totalSessions)
                        .build())
                .collect(Collectors.toList());
    }

    // 5. 세션 종료 처리 (엔딩)
    @Transactional
    public ReportDto.SessionEndResponse endSession(String sessionId) {
        // 실제 운영 시에는 여기서 호감도 계산 및 세션 상태 변경 로직이 들어갑니다.
        // 현재는 종료되었다는 것을 확정하는 로직 수행
        return ReportDto.SessionEndResponse.builder()
                .session_id(sessionId)
                .final_affinity(85) // 예시값
                .achieved_level("B2") // 예시값
                .build();
    }
}