// 트랙 A - 대화 턴이 몰릴 때 무관한 API(로그인/홈)가 같이 막히는지.
//
// 트러블슈팅_턴_처리_커넥션_점유의 증상 그대로다: 문제는 "턴이 느리다"가 아니라
// "턴 때문에 로그인이 막힌다"이므로, 턴 부하(turns)와 무관 API 프로브(probe_*)를
// 동시에 돌리고 프로브의 p95를 버전 간 비교한다.
//
// 전제: 서버가 perf 프로파일(느린 LLM 대역, PERF_LLM_DELAY_MS)로 떠 있을 것.
//
// 실행 예:
//   k6 run -e BASE=http://localhost:8091 \
//          --summary-export perf/results/turn-v1/turn-load.json \
//          perf/k6/login-under-turn-load.js
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const BASE = __ENV.BASE || 'http://localhost:8091';
const EMAIL = __ENV.EMAIL || 'demo@goodquestion.kr';
const PASSWORD = __ENV.PASSWORD || 'demo1234!';
const CHILD_ID = __ENV.CHILD_ID || 'aaaaaaaa-aaaa-aaaa-aaaa-000000000001';
const TURN_VUS = Number(__ENV.TURN_VUS || 8);
const DURATION = __ENV.DURATION || '90s';

const probeLogin = new Trend('probe_login_duration', true);
const probeHome = new Trend('probe_home_duration', true);
const probeLoginOk = new Rate('probe_login_ok');
const probeHomeOk = new Rate('probe_home_ok');
const turnDuration = new Trend('turn_duration', true);
const turnOk = new Rate('turn_ok');

export const options = {
    scenarios: {
        turns: {
            executor: 'constant-vus',
            vus: TURN_VUS,
            duration: DURATION,
            exec: 'turnLoop',
        },
        probe_login: {
            executor: 'constant-arrival-rate',
            rate: 1, timeUnit: '1s',
            duration: DURATION,
            preAllocatedVUs: 10, maxVUs: 30,
            exec: 'loginProbe',
        },
        probe_home: {
            executor: 'constant-arrival-rate',
            rate: 2, timeUnit: '1s',
            duration: DURATION,
            preAllocatedVUs: 10, maxVUs: 30,
            exec: 'homeProbe',
        },
    },
};

// 로그인 응답이 버전에 따라 {accessToken} 또는 {tokens:{accessToken}} 이다
function login() {
    const res = http.post(`${BASE}/api/auth/login`,
        JSON.stringify({ email: EMAIL, password: PASSWORD }),
        { headers: { 'Content-Type': 'application/json' } });
    if (res.status !== 200) return null;
    const body = res.json();
    return (body.tokens && body.tokens.accessToken) || body.accessToken || null;
}

function authHeaders(token) {
    return { headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` } };
}

export function setup() {
    const token = login();
    if (!token) throw new Error('로그인 실패 - 시드 데모 계정 확인');
    // 이야기 id는 API로 찾는다 (버전 간 시드 차이에 안전). 실패 시 시드 상수.
    let storyId = '11111111-1111-1111-1111-111111111111';
    const stories = http.get(`${BASE}/api/stories`, authHeaders(token));
    if (stories.status === 200) {
        const list = stories.json();
        if (Array.isArray(list) && list.length > 0 && list[0].id) storyId = list[0].id;
        else if (list.stories && list.stories.length > 0) storyId = list.stories[0].id;
    }
    return { token, storyId };
}

// 세션을 만들고 발화가 받아들여지는 장면까지 진행한다.
// 첫 장면이 스토리 장면이면 story-complete로 넘긴다 (최대 6장면).
function openDialogueSession(token, storyId) {
    const start = http.post(`${BASE}/api/children/${CHILD_ID}/sessions`,
        JSON.stringify({ storyId }), authHeaders(token));
    if (start.status !== 200 && start.status !== 201) return null;
    const sessionId = start.json().sessionId || start.json().id;
    if (!sessionId) return null;

    for (let i = 0; i < 6; i++) {
        const probe = submitUtterance(token, sessionId, false);
        if (probe === 'ok') return sessionId;
        if (probe === 'not-dialogue') {
            http.post(`${BASE}/api/sessions/${sessionId}/scenes/current/story-complete`,
                null, authHeaders(token));
            continue;
        }
        return null;
    }
    return null;
}

// 반환: 'ok' | 'not-dialogue' | 'error'
function submitUtterance(token, sessionId, record) {
    const t0 = Date.now();
    const res = http.post(`${BASE}/api/sessions/${sessionId}/utterances`,
        JSON.stringify({ text: '방귀 소리가 정말 컸어요' }),
        Object.assign({ timeout: '30s' }, authHeaders(token)));
    if (record) {
        turnDuration.add(Date.now() - t0);
        turnOk.add(res.status === 200);
    }
    if (res.status === 200) return 'ok';
    try {
        const code = res.json().code;
        if (code === 'SCENE_NOT_DIALOGUE' || code === 'SCENE_NOT_STORY') return 'not-dialogue';
        if (code === 'MAX_TURNS_EXCEEDED') return 'not-dialogue';
    } catch (_) { /* 본문 없음 */ }
    return 'error';
}

export function turnLoop(data) {
    const sessionId = openDialogueSession(data.token, data.storyId);
    if (!sessionId) { sleep(1); return; }
    for (let i = 0; i < 3; i++) {
        submitUtterance(data.token, sessionId, true);
    }
    http.post(`${BASE}/api/sessions/${sessionId}/stop`, null, authHeaders(data.token));
}

export function loginProbe() {
    const t0 = Date.now();
    const res = http.post(`${BASE}/api/auth/login`,
        JSON.stringify({ email: EMAIL, password: PASSWORD }),
        { headers: { 'Content-Type': 'application/json' }, timeout: '15s' });
    probeLogin.add(Date.now() - t0);
    probeLoginOk.add(res.status === 200);
    check(res, { 'login 200': (r) => r.status === 200 });
}

export function homeProbe(data) {
    const t0 = Date.now();
    const res = http.get(`${BASE}/api/children/${CHILD_ID}/home`,
        Object.assign({ timeout: '15s' }, authHeaders(data.token)));
    probeHome.add(Date.now() - t0);
    probeHomeOk.add(res.status === 200);
}
