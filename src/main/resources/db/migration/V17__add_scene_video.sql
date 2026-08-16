-- 장면 영상 (스토리지 경로). null이면 image_url만 쓴다.
-- 영상은 이미지를 대체하지 않고 얹는다 - 재생 실패·저사양·데이터 절약 모드는 image_url로 폴백한다.
alter table story_scenes add column if not exists video_url varchar(255);
