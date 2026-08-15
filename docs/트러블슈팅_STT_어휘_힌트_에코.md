# 트러블슈팅: STT가 어휘 힌트를 그대로 돌려줘 아이 말풍선에 단어 목록이 뜨는 문제

이야기 진행 중 아이 말풍선에 "며느리, 시아버지, 방귀, 친정, 갓, 이장, 배나무,
장대, 기왓장"이 그대로 표시되던 문제다. 이 문자열은 오인식 방지용으로 STT 요청의
prompt에 실어 보내는 어휘 힌트 그 자체다. 원인은 한 겹이 아니었다. 웹 녹음의
샘플레이트 불일치가 알아들을 수 없는 오디오를 만들었고(입력), 그런 오디오에서
모델이 prompt를 복창하는 환각이 일어났고(모델), 프론트가 STT 결과를 검증 없이
즉시 제출하고 표시해서(경로) 화면과 DB까지 그대로 도달했다. 세 겹을 각각 고치고
방어선을 겹으로 두었다.

- 상태: 해결 (남은 것: 아동 실녹음 검증 미결-01, 이미 저장된 에코 발화 정리)
- 관련 코드: `OpenAiSttClient`, 프론트 `mission_voice_recorder.dart`, `play_view.dart`
- 회귀 테스트: `VocabularyEchoTest` (9건), 프론트 `play_view_test.dart` (확인 단계, 60초 상한)

---

## 1. 증상

**아이가 멀쩡히 말했는데 말풍선에 힌트 단어 목록이 뜬다.**

- "내가 한 말" 말풍선에 힌트 목록이 그대로 표시된다
- 그 텍스트가 발화로 제출돼 `messages`에 아이 말로 저장된다
- 캐릭터가 단어 목록에 대답하는 턴이 돌아간다
- 보호자 리포트의 대표 발화 후보에도 오를 수 있다

`stt_confidence`는 오히려 높게 찍힌다. 모델이 prompt를 복창할 때는 토큰 logprob이
높아서, 저신뢰 컷(0.5)으로는 걸러지지 않는다.

## 2. 원인: 세 겹이 겹쳐야 화면까지 도달한다

### 원인 1 (입력): 웹 녹음의 샘플레이트 불일치

프론트는 `record` 패키지에 16kHz 녹음을 요청하고, 직접 만든 WAV 헤더에도
16000을 적는다. 네이티브(iOS/Android)는 OS가 리샘플링까지 해 줘서 요청대로
받지만, 웹의 `record_web`은 `getUserMedia`가 브라우저와 협상한 실제 샘플레이트
(보통 하드웨어 기본값 48000)로 `AudioContext`를 다시 연다. 이 변경은 조용히
일어나고, 협상된 값을 앱 코드가 조회할 공개 API도 없다.

결과: 48kHz로 찍힌 데이터에 16kHz라고 적힌 헤더가 붙어 나간다. 서버와 벤더는
헤더를 믿으므로 같은 데이터를 3배 긴 시간으로 펼쳐 해석한다. 재생 속도가 3배
느려지고 음높이가 1.5옥타브 내려간 소리, 즉 사람 말로 들리지 않는 입력이 된다.

| | 실제 | 서버가 이해한 것 |
|---|---|---|
| 길이 | 2.97초 | 8.91초 |
| 속도 | 정상 | 3배 느림 |

### 원인 2 (모델): 알아들을 수 없는 오디오에서 prompt 복창 환각

Whisper 계열의 알려진 실패 모드다. 무음이거나 인식할 발화가 없으면 모델이
오디오 대신 prompt에 기대어 출력을 만든다. 실측으로 확인한 유발 조건과 출력:

| 입력 | 출력 |
|---|---|
| 완전 무음 1.5초 | 힌트 목록 전체를 그대로 복창 |
| 0.25초 조각 | 힌트 목록 전체를 그대로 복창 |
| 미세 잡음 1초 | 힌트 목록 전체를 그대로 복창 |
| 샘플레이트 불일치(뭉개진 소리) | 목록 복창, 또는 3회 중 1회꼴로 **재조합 문장** |
| 음량 1/20로 줄인 정상 발화 | 정상 인식 (작은 소리는 문제가 아니다) |

재조합 문장이 위험하다. "며느리와 시아버지가 방귀를 뀌는 사이, 친정에서 갓을
쓴 이장이 배나무를 잘라 기왓장을 쌓았다."처럼 힌트 어휘를 조사와 서술어로 이어
말이 되는 문장을 만든다. 최초 필터(완전 일치 비교)를 그대로 통과했다.

어휘를 쉼표 나열로 주는 prompt 형태가 이 환각을 가장 잘 유발한다는 것도 실측과
문헌이 일치했다.

### 원인 3 (경로): 프론트가 STT 결과를 무검증 즉시 제출

프론트는 `/api/stt`의 text를 받자마자 발화로 제출하고 말풍선에 표시했다.
설계 문서(트러블슈팅_STT_신뢰도_산출.md 3절)는 "저신뢰 안내가 의미 있는 유일한
시점은 제출 전"이라고 정해 뒀지만, 그 시점에 아이가 결과를 확인할 자리가
없었다. 벤더가 무엇을 주든 화면과 DB에 즉시 박히는 구조였다.

## 3. 분석: 재현 방법

원인 1은 오디오 파일 조작으로 재현했다. 같은 48kHz PCM에 헤더만 다르게 붙여
벤더에 직접 넣으면:

```
헤더 48000 (올바름) -> "며느리가 방귀를 뀌어서 배가 떨어졌어요."
헤더 16000 (웹 실제) -> 힌트 목록 복창 또는 재조합 문장
```

원인 2는 무음/잡음/초단시간 WAV로 재현된다. 신뢰도 파이프라인이 못 잡는 것도
확인했다. 복창 출력의 confidence가 0.9 이상으로 나온다.

## 4. 적용한 것: 원인 제거와 겹 방어

한 겹이 새더라도 다음 겹이 잡도록 다섯 가지를 나눠 적용했다.

### 4-1. 웹 샘플레이트 분기 (프론트, 원인 제거)

`_sampleRate = kIsWeb ? 48000 : 16000`. 브라우저가 실제로 쓸 값을 처음부터
요청해 헤더와 데이터의 불일치 자체를 없앤다. 고칠 지점은 "브라우저가 다른 값을
쓴다"가 아니라 "우리가 그 값을 안 따라간다"였다.

한계: `record_web`이 협상값을 노출하지 않아 "웹은 48000"이라는 가정에
의존한다. 48000이 아닌 하드웨어에서는 재발할 수 있다.

### 4-2. 에코 판정 확장 (백엔드 PR #57)

`isVocabularyEcho`를 완전 일치에서 두 갈래로 확장했다.

- **나열 에코**: 결과에서 힌트 단어를 전부 지웠을 때 아무것도 남지 않으면 에코.
  전체 반복, 일부 나열, 순서 바뀜을 한 번에 잡는다. 아이의 한 단어 답변
  ("방귀!")을 지키기 위해 힌트 단어 2개 이상일 때만 적용한다
- **재조합 에코**: 서로 다른 힌트 단어의 등장 비율이 2/3 이상이면 에코. 힌트는
  전 장면 어휘의 합집합이라 진짜 발화가 이만큼 폭넓게 쓸 일이 없다(실측:
  재조합 에코는 9개 중 8개 등장, 정상 발화는 2개 수준)

오판(정상 발화를 에코로 봄)의 비용은 "다시 말해 볼까?" 안내 한 번이고, 놓침의
비용은 아이가 하지 않은 말의 저장이라 기준을 이렇게 뒀다. 에코로 판정되면 빈
결과로 돌려 기존 `STT_EMPTY_TEXT`(422) 경로를 탄다.

### 4-3. 문장형 프롬프트 (백엔드 PR #61, 유발 감소)

`external.stt.prompt` 설정을 추가해 벤더에는 같은 어휘 9개를 이야기 문장에
녹인 프롬프트를 보낸다. 나열형은 vocabulary-hint로 남겨 에코 판정의 어휘
목록으로 계속 쓴다.

실측에서 중요한 시너지가 확인됐다. 무음이 들어오면 모델이 문장형 프롬프트를
그대로 복창하는데, 문장에 어휘가 전부 녹아 있어 4-2의 재조합 판정에 정확히
걸린다. 프롬프트를 문장형으로 바꿔도 에코 방어가 뚫리지 않는다는 것을
테스트로 고정했다(`VocabularyEchoTest.문장형_프롬프트를_복창해도_에코다`).

### 4-4. 변환 결과 확인 단계 (프론트 PR #23, 마지막 방어선)

STT 결과를 받자마자 제출하지 않고 "이렇게 들었어요" 확인 화면을 거쳐
"맞아요"를 눌러야 턴이 나간다. 저신뢰면 "이렇게 들었는데, 맞을까요?"로 묻는다.
"다시 말할래요"는 결과를 버리고 곧바로 재녹음하며 sttRetryCount를 올린다.

서버 필터를 빠져나온 에코가 있어도 아이가 확인하는 단계에서 걸러질 수 있고,
설계가 정한 "제출 전 안내" 시점도 이제 지켜진다.

### 4-5. 녹음 60초 상한 (프론트 PR #22, 같은 실측에서 나온 별건)

웹이 48kHz가 되면서 초당 데이터가 3배(약 94KB)가 됐고, 109초를 넘기면
멀티파트 한도 10MB에 걸려 업로드가 통째로 413이 된다. 60초에서 자동 종료하고
거기까지 말한 것을 서버로 보낸다.

## 5. 남은 것

- **아동 실녹음 검증(미결-01)**: 지금까지의 실측은 전부 성인 톤 합성음이다
- **이미 저장된 에코 발화**: 필터 배포 전에 저장된 것은 그대로 남아 있다.
  정리 절차는 7절에 있다(운영 DB 접근이 필요해 아직 실행 전이다)
- **장면별 힌트**: 장면별 proper_nouns를 실어 보내는 구조가 정석이지만
  /api/stt 계약에 장면 정보가 없어 보류 상태다. 계약 변경이 필요하면 함께
  정한다(API 명세 6절 미결)
- **웹 48000 가정**: record_web이 협상값을 노출하면 실제 값을 따라가도록
  바꾼다

## 6. 교훈

- **벤더 출력은 신뢰 경계 밖이다.** STT 텍스트를 아이 발화로 승격시키는 지점에
  검증(서버 필터)과 확인(아이 승인)이 둘 다 필요했다
- **신뢰도는 환각을 못 잡는다.** 복창은 모델 입장에서 확신에 찬 출력이라
  logprob 기반 신뢰도가 높게 나온다. 저신뢰 컷과 에코 필터는 서로 대체재가
  아니라 별개의 방어다
- **헤더와 데이터가 따로 놀 수 있는 포맷은 만든 쪽이 검증해야 한다.** WAV
  헤더를 손으로 만들면 그 값이 데이터의 실제 속성과 일치하는지는 아무도
  검사해 주지 않는다

## 7. 이미 저장된 에코 발화 정리 절차

필터 배포 전에 저장된 에코는 그대로 남아 있다. 아래 SQL은 `isVocabularyEcho`와
같은 판정을 옮긴 것이고, 로컬 DB에서 에코 3종(전체 나열, 재조합 문장, 부분 나열)과
정상 발화 4종(정상 문장 2, 한 단어 답변 1, 힌트 단어가 섞인 정상 발화 1)으로
검증했다. 검증에서 에코 3건만 잡히고 정상 발화와 캐릭터 대사는 걸리지 않았다.

**힌트 목록은 `external.stt.vocabulary-hint` 설정과 같아야 한다.** 설정을 바꿨으면
아래 배열도 함께 바꾼다.

### 7-1. 판정 뷰

```sql
create or replace view v_stt_hint_echo as
with hint as (
    select array['며느리','시아버지','방귀','친정','갓','이장','배나무','장대','기왓장']::text[] as words
),
child as (
    select m.id, m.session_id, m.turn_order, m.text, m.stt_low_confidence,
           m.stt_confidence, m.created_at,
           regexp_replace(m.text, '[[:space:],.·!?''"]', '', 'g') as normalized
    from messages m
    where m.speaker_type = 'CHILD'
),
scored as (
    select c.*,
           (select count(*) from unnest(h.words) w where position(w in c.normalized) > 0) as matched,
           (select regexp_replace(
                       c.normalized,
                       array_to_string(array(select w from unnest(h.words) w order by length(w) desc), '|'),
                       '', 'g')) as remainder,
           array_length(h.words, 1) as hint_size
    from child c, hint h
)
select id, session_id, turn_order, text, stt_low_confidence, stt_confidence, created_at, matched,
       case when matched >= 2 and remainder = '' then '나열' else '재조합' end as echo_kind
from scored
where (matched >= 2 and remainder = '')
   or (hint_size >= 3 and matched >= ceil(hint_size * 2.0 / 3));
```

긴 단어부터 지우는 것이 중요하다. 짧은 단어가 긴 단어의 일부면 조각이 남아
나열 판정이 어긋난다(자바 구현이 `length desc`로 정렬하는 이유와 같다).

### 7-2. 대상 확인 (읽기만 한다)

```sql
-- 종류별 건수
select echo_kind, count(*), min(created_at), max(created_at)
from v_stt_hint_echo group by echo_kind;

-- 세션 상태별. 진행 중 세션은 아이가 다시 들어오면 말풍선에 복원된다
select s.status, count(*) as 발화수, count(distinct e.session_id) as 세션수
from v_stt_hint_echo e join story_sessions s on s.id = e.session_id
group by s.status;

-- 이미 생성된 리포트에 섞였는지. 있으면 리포트 재생성을 검토한다
select r.id, r.session_id, count(*) from v_stt_hint_echo e
join reports r on r.session_id = e.session_id group by r.id, r.session_id;

-- 전문
select id, session_id, turn_order, echo_kind, stt_confidence, created_at, text
from v_stt_hint_echo order by created_at;
```

### 7-3. 백업

```sql
create table if not exists messages_hint_echo_backup as
select m.*, now() as backed_up_at
from messages m where m.id in (select id from v_stt_hint_echo);
```

### 7-4. 정리 - 행을 지우지 않고 저신뢰로 표시한다

```sql
begin;
update messages set stt_low_confidence = true
where id in (select id from v_stt_hint_echo) and stt_low_confidence = false;
-- 건수가 7-2에서 본 것과 맞으면 commit, 아니면 rollback
```

`stt_low_confidence = true`면 `ReportService`가 대표 발화 후보(`ReportService.java:100`)와
요소 근거(`:181`)에서 제외한다. 보호자에게 아이가 하지 않은 말이 실리는 문제,
즉 이 사고의 실질적 피해가 이것으로 막힌다.

**행을 지우지 않는 이유가 셋이다.**

1. `utterance_analyses.message_id`가 `on delete cascade`라 분석 기록이 함께 사라진다
2. `story_sessions.current_child_turn_count`는 메시지에서 세는 값이 아니라 별도
   저장 컬럼이다. 메시지만 지우면 턴 수가 어긋나고, 진행 판단과 최대 턴 종료가
   그 값을 본다
3. 그 발화에 대답한 캐릭터 대사가 남아 대화가 앞뒤로 안 맞게 된다

진행 중 세션의 말풍선 복원까지 없애야 한다면, 행을 지우는 대신 그 세션을
그만하기(`POST /api/sessions/{id}/stop`)로 종료시키는 편이 정합성이 깨지지 않는다.

### 7-5. 되돌리기

```sql
update messages m set stt_low_confidence = b.stt_low_confidence
from messages_hint_echo_backup b where m.id = b.id;
```
