import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    vus: 30,
    duration: '30s',      // 30초 동안 테스트 진행
};

// 💡 만약 로컬 터미널에서 k6 명령어를 친다면 localhost로,
// 도커 내부 네트워크에서 실행한다면 'http://backend:8080'으로 유지하세요!
const BASE_URL = 'http://localhost:8080';

export default function () {
    // 1. 세션 생성
    const sessionUrl = `${BASE_URL}/api/chat/sessions`;
    const sessionPayload = JSON.stringify({
        userId: 44,
        stageId: 1,
        characterId: "ian",
        scenarioId: ""
    });

    const jsonHeaders = { 'Content-Type': 'application/json' };
    let sessionRes = http.post(sessionUrl, sessionPayload, { headers: jsonHeaders });

    if (sessionRes.status === 200) {
        // 안전하게 데이터 추출
        const resBody = sessionRes.json();
        const sessionId = resBody.data.sessionId;

        // 2. 메시지 전송
        const chatUrl = `${BASE_URL}/api/chat/message`;

        // k6에서 multipart/form-data를 보낼 때는 body를 객체(Object) 형태로 감싸서 던집니다.
        const chatMultipartData = {
            // 가짜 오디오 바이너리 데이터를 파일 형태로 생성해서 꽂아줍니다 (백엔드 MultipartFile 수신용)
            audioFile: http.file('dummy_voice_binary_stream_data_content', 'load_test_voice.wav', 'audio/wav'),

            // 백엔드가 @RequestParam("request")로 가로채서 가공할 JSON 문자열 파라미터
            request: JSON.stringify({
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
            })
        };

        let chatRes = http.post(chatUrl, chatMultipartData);

        // 결과 검증
        check(chatRes, {
            'chat HTTP 200 OK 성공': (r) => r.status === 200
        });
    }

    sleep(1);
}