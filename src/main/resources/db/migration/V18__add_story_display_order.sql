-- 목록과 홈 추천의 이야기 노출 순서를 콘텐츠가 정한다. 작을수록 앞이다.
--
-- 지금까지 정렬 키는 created_at desc 하나뿐이었는데 시드는 created_at을 적지 않아
-- DB 기본값 now()가 들어간다. 포스트그레스의 now()는 트랜잭션 시작 시각이라 한
-- 마이그레이션에서 함께 들어간 이야기들이 같은 값을 갖고, 그 사이 순서는 실행 계획에
-- 따라 갈린다. 진행 가능한 이야기를 첫 칸에 고정할 방법이 없었다.
--
-- 기본값 100은 순서를 지정하지 않고 추가된 이야기를 큐레이션한 이야기 뒤에 놓는다.
-- 같은 값끼리는 기존대로 created_at desc로 갈린다.
alter table stories add column if not exists display_order smallint not null default 100;
