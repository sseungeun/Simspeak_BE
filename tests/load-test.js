import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// ───────────────────────────────────────────────────────────
// 진단용 커스텀 메트릭: 어느 단계에서 깨지는지 분리해서 본다
// ───────────────────────────────────────────────────────────
const sessionSuccess   = new Rate('session_success_rate');    // 세션 생성 성공률
const sessionFailCount = new Counter('session_fail_count');   // 세션 실패 누적 횟수
const sessionDuration  = new Trend('session_duration', true); // 세션 생성 응답시간(AI 미포함, 순수 백엔드+DB)
const messageSuccess   = new Rate('message_success_rate');    // 메시지 성공률

export const options = {
    // ⚠️ 진단 단계: VU를 낮춰 k6/백엔드 로그를 읽기 쉽게.
    //    원인 잡은 뒤 5 → 10 → 30으로 올려가며 실패율이 동시성에 비례하는지 확인.
    vus: 5,
    duration: '30s',

    // 태그별 응답시간을 요약에 노출시키기 위한 threshold (값은 참고용, 자유롭게 조정)
    thresholds: {
        'http_req_duration{name:create_session}': ['p(95)<1000'], // 세션 생성은 1초 안에 끝나야 정상
        'http_req_duration{name:send_message}':   ['p(95)<8000'], // 메시지는 AI 지연 포함이라 느림
        'session_success_rate': ['rate>0.95'],                     // 세션 성공률 95% 미만이면 실패로 표시
    },
};

// 로컬 k6 → 기본값(localhost) 사용 / 도커 k6 → BASE_URL=http://backend:8080 주입
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
    // ── 1. 세션 생성 ────────────────────────────────────────
    const sessionUrl = `${BASE_URL}/api/chat/sessions`;
    const sessionPayload = JSON.stringify({
        userId: 44,
        stageId: 1,
        characterId: 'ian',
        scenarioId: '',
    });
    const jsonHeaders = { 'Content-Type': 'application/json' };

    const sessionRes = http.post(sessionUrl, sessionPayload, {
        headers: jsonHeaders,
        tags: { name: 'create_session' }, // ◀ 이 요청만 따로 집계
    });

    const sessionOk = sessionRes.status === 200;
    sessionSuccess.add(sessionOk);
    sessionDuration.add(sessionRes.timings.duration);

    check(sessionRes, {
        'session: status 200': (r) => r.status === 200,
        'session: has sessionId': (r) => {
            try { return r.json('data.sessionId') != null; }
            catch (e) { return false; }
        },
    });

    // 🔍 실패 원인 캡처 — 상태코드 + 응답 바디를 찍는다.
    //    콘솔 폭주 방지를 위해 VU 1번의 처음 10회만 로깅.
    if (!sessionOk) {
        sessionFailCount.add(1);
        if (__VU === 1 && __ITER < 10) {
            console.error(
                `[SESSION FAIL] status=${sessionRes.status} | ` +
                `body=${String(sessionRes.body).slice(0, 300)}`
            );
        }
        sleep(1);
        return; // 세션 실패면 메시지 단계로 안 넘어감
    }

    // ── 2. 메시지 전송 ──────────────────────────────────────
    let sessionId;
    try {
        sessionId = sessionRes.json().data.sessionId;
    } catch (e) {
        console.error(`[SESSION PARSE FAIL] body=${String(sessionRes.body).slice(0, 300)}`);
        sleep(1);
        return;
    }

    const chatUrl = `${BASE_URL}/api/chat/message`;
    const chatMultipartData = {
        audioFile: http.file('dummy_voice_binary_stream_data_content', 'load_test_voice.wav', 'audio/wav'),
        request: JSON.stringify({
            sessionId: sessionId,
            text: 'I felt so moved when you gave me that coffee.',
            inputType: 'text',
            characterId: 'ian',
            scenarioId: '',
            targetLanguage: 'en-US',
            stageLevel: 1,
            userLevel: 'B2',
            turnCount: 5,
            currentAffinity: 30,
            userAudioUrl: '',
            history: [
                { role: 'user', text: 'Hey, how is your day going?' },
                { role: 'assistant', text: "It's been great! Just had some coffee. How about you?" },
            ],
        }),
    };

    const chatRes = http.post(chatUrl, chatMultipartData, {
        tags: { name: 'send_message' }, // ◀ 메시지 요청도 따로 집계
    });

    const msgOk = chatRes.status === 200;
    messageSuccess.add(msgOk);

    check(chatRes, {
        'message: status 200': (r) => r.status === 200,
    });

    if (!msgOk && __VU === 1 && __ITER < 10) {
        console.error(
            `[MESSAGE FAIL] status=${chatRes.status} | ` +
            `body=${String(chatRes.body).slice(0, 300)}`
        );
    }

    sleep(1);
}