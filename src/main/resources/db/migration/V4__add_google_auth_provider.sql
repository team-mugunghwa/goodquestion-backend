alter table parents drop constraint if exists parents_provider_check;
alter table parents
    add constraint parents_provider_check
    check (provider in ('LOCAL', 'KAKAO', 'GOOGLE'));
