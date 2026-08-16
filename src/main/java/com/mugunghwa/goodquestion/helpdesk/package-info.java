/**
 * 고객 지원 - 공지사항, 이용안내, 문의, 알림, 푸시 기기 토큰.
 *
 * <p>패키지 이름이 support가 아닌 것은 테스트 쪽에 이미 같은 이름이 있어서다
 * ({@code src/test/.../support}는 Testcontainers 설정 같은 테스트 지원 코드다).
 * 한 이름이 소스 세트에 따라 다른 뜻을 갖게 두면 import 한 줄을 볼 때마다 헷갈린다.
 *
 * <p>이 패키지의 데이터는 대부분 <b>관리자 콘솔</b>(admin-goodquestion-backend)이 쓰고
 * 여기가 읽는다. 두 앱이 같은 PostgreSQL을 본다. 공지를 공개하거나 문의에 답변을 다는
 * 것은 그쪽이고, 이쪽은 사용자에게 보여주는 일과 사용자가 만드는 것(문의, 기기 토큰)만
 * 맡는다.
 *
 * <p>의존은 {@code user}와 {@code global}까지다. 이야기나 학습 도메인을 참조하지 않는다 -
 * 고객 지원은 서비스 기능과 독립적으로 동작해야 하고, 실제로 문의 화면은 아이 프로필이
 * 없어도 열린다.
 */
package com.mugunghwa.goodquestion.helpdesk;
