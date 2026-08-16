-- 예문 3종 체계 - 단어마다 예문을 셋 갖는다.
--
--   example_sentence : 1) 이야기 예문. 아이가 단어를 만난 대사 문장(요청 우선)
--                      또는 이야기 상황을 반영한 문장
--   example_daily    : 2) 일상 예문. 이야기 밖 일상에서 어떻게 쓰는지
--   example_advanced : 3) 심화 예문. 2)보다 한 단계 어려운 문장
--
-- 기존 example_sentence 컬럼은 1) 슬롯으로 그대로 쓴다(하위호환).
-- V14 이전에 저장된 행은 2)/3)이 null이다.
alter table wordbook add column example_daily varchar(300);
alter table wordbook add column example_advanced varchar(300);

alter table story_vocabulary add column example_daily varchar(300);
alter table story_vocabulary add column example_advanced varchar(300);

comment on column wordbook.example_daily is '일상 예문 - 이야기 밖 쓰임';
comment on column wordbook.example_advanced is '심화 예문 - 일상 예문보다 어려운 문장';
comment on column story_vocabulary.example_daily is '일상 예문 - 이야기 밖 쓰임';
comment on column story_vocabulary.example_advanced is '심화 예문 - 일상 예문보다 어려운 문장';
