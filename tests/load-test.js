import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    vus: 5,               // 동시에 5명 접속
    duration: '30s',      // 30초 동안 테스트
};

const headers = { 'Content-Type': 'application/json' };

export default function () {
    // 1. 세션 생성 (필수 필드 매핑)
    const sessionUrl = 'http://backend:8080/api/chat/sessions';
    const sessionPayload = JSON.stringify({
        userId: 44,
        stageId: 1,
        characterId: "ian",
        scenarioId: ""
    });

    let sessionRes = http.post(sessionUrl, sessionPayload, { headers });

    if (sessionRes.status === 200) {
        const sessionId = sessionRes.json().data.sessionId;

        // 2. 메시지 전송
        const chatUrl = 'http://backend:8080/api/chat/message';
        const chatPayload = JSON.stringify({
            sessionId: sessionId,
            text: "I felt so moved when you gave me that coffee.",
            inputType: "text",
            characterId: "ian",
            scenarioId: "",
            targetLanguage: "en-US",
            stageLevel: 1,
            userLevel: "B2",
            turnCount: 5,
            currentAffinity: 30,
            userAudioUrl: "",
            history: [
                { "role": "user", "text": "Hey, how is your day going?" },
                { "role": "assistant", "text": "It's been great! Just had some coffee. How about you?" }
            ]
        });

        let chatRes = http.post(chatUrl, chatPayload, { headers });
        check(chatRes, { 'chat success': (r) => r.status === 200 });
    }

    sleep(1);
}