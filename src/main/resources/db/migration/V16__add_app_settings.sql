-- ============================================================
-- app_settings - 재배포 없이 바꾸는 런타임 설정
--
-- 첫 용도는 TTS 벤더 전환(tts.vendor = OPENAI | GEMINI | CHIRP3)이다.
-- Gemini 무료 등급은 분당 호출 한도가 빠듯해 테스트 중 크레딧이 녹는다 -
-- 테스트 기간에는 Chirp 3: HD(월 100만 자 무료)로 내렸다가 시연 때 올린다.
--
-- 쓰는 쪽은 관리자 콘솔(admin-goodquestion-backend), 읽는 쪽은 이 서버다.
-- 행이 없으면 서버는 환경변수 기본값(TTS_VENDOR, 없으면 openai)으로 동작한다 -
-- 관리자 콘솔이 없는 로컬·CI에서도 지금과 똑같이 뜬다.
-- ============================================================
create table app_settings (
    key        varchar(64)  primary key,
    value      varchar(128) not null,
    updated_at timestamptz  not null default now()
);

comment on table app_settings is '재배포 없이 바꾸는 런타임 설정. 관리자 콘솔이 쓰고 본 서버가 읽는다';
