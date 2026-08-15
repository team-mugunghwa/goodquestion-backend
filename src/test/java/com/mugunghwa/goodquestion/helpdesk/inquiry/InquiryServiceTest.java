package com.mugunghwa.goodquestion.helpdesk.inquiry;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import com.mugunghwa.goodquestion.helpdesk.inquiry.dto.InquiryDtos.CreateInquiryRequest;
import com.mugunghwa.goodquestion.helpdesk.inquiry.dto.InquiryDtos.InquiryDetailResponse;
import com.mugunghwa.goodquestion.helpdesk.notification.NotificationRepository;
import com.mugunghwa.goodquestion.support.IntegrationTest;
import com.mugunghwa.goodquestion.user.auth.AuthProvider;
import com.mugunghwa.goodquestion.user.parent.Parent;
import com.mugunghwa.goodquestion.user.parent.ParentRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 문의 작성부터 답변 확인까지. 답변을 등록하는 것은 관리자 콘솔이라 이 테스트에서는
 * 그쪽이 하는 일을 SQL로 대신한다 - 두 앱이 같은 테이블을 보는 구조가 실제로 맞는지가
 * 여기서 검증된다.
 */
@IntegrationTest
@Transactional
class InquiryServiceTest {

    @Autowired InquiryService inquiryService;
    @Autowired ParentRepository parentRepository;
    @Autowired NotificationRepository notificationRepository;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired EntityManager entityManager;

    private UUID parentId;

    @BeforeEach
    void setUp() {
        Parent parent = parentRepository.save(Parent.builder()
                .email("inquiry-%s@example.com".formatted(System.nanoTime()))
                .passwordHash("hash")
                .provider(AuthProvider.LOCAL)
                .name("김보호")
                .build());
        parentId = parent.getId();
    }

    @Test
    @DisplayName("문의를 만들면 답변 대기 상태로 저장되고 답변은 비어 있다")
    void createStartsPending() {
        InquiryDetailResponse created = inquiryService.create(parentId,
                new CreateInquiryRequest(InquiryCategory.BUG, "소리가 안 나요", "이야기 재생이 무음입니다."));

        assertThat(created.status()).isEqualTo(InquiryStatus.PENDING);
        assertThat(created.answer()).isNull();
    }

    @Test
    @DisplayName("관리자가 답변을 달면 사용자 상세에 그 내용이 보인다")
    void answerBecomesVisible() {
        InquiryDetailResponse created = inquiryService.create(parentId,
                new CreateInquiryRequest(InquiryCategory.ETC, "질문", "본문"));

        // JPA가 만든 행을 SQL이 보려면 먼저 밀어내야 한다. 같은 트랜잭션이라도
        // 영속성 컨텍스트에만 있으면 JdbcTemplate에는 보이지 않는다.
        entityManager.flush();

        // 관리자 콘솔이 하는 일. 같은 DB의 같은 테이블에 쓴다.
        jdbcTemplate.update("""
                insert into inquiry_answers (inquiry_id, admin_name, content)
                values (?, '고객센터', '확인 후 조치했습니다.')
                """, created.id());
        jdbcTemplate.update(
                "update inquiries set status = 'ANSWERED', answered_at = now() where id = ?",
                created.id());
        // SQL로 바꾼 값은 이미 읽어 둔 엔티티에 반영되지 않는다. 비워야 다시 읽는다.
        entityManager.clear();

        InquiryDetailResponse detail = inquiryService.get(parentId, created.id());
        assertThat(detail.status()).isEqualTo(InquiryStatus.ANSWERED);
        assertThat(detail.answer()).isNotNull();
        assertThat(detail.answer().content()).isEqualTo("확인 후 조치했습니다.");
        assertThat(detail.answer().adminName()).isEqualTo("고객센터");
    }

    @Test
    @DisplayName("답변 알림이 쌓여 있으면 알림함에서도 확인할 수 있다")
    void notificationIsVisible() {
        InquiryDetailResponse created = inquiryService.create(parentId,
                new CreateInquiryRequest(InquiryCategory.ETC, "질문", "본문"));
        entityManager.flush();
        jdbcTemplate.update("""
                insert into notifications (parent_id, type, title, body, link_path)
                values (?, 'INQUIRY_ANSWERED', '답변이 등록되었습니다', '눌러서 확인해 주세요', ?)
                """, parentId, "/support/" + created.id());

        // 푸시가 막혀 있어도 여기가 남아 있어야 사용자가 답변을 알 수 있다.
        assertThat(notificationRepository.countByParentIdAndReadAtIsNull(parentId)).isEqualTo(1);
    }

    @Test
    @DisplayName("남의 문의는 찾을 수 없다고 답한다")
    void cannotReadOthersInquiry() {
        InquiryDetailResponse created = inquiryService.create(parentId,
                new CreateInquiryRequest(InquiryCategory.ETC, "질문", "본문"));
        UUID stranger = UUID.randomUUID();

        // 403이면 그 id의 문의가 존재한다는 사실이 새어 나간다.
        assertThatThrownBy(() -> inquiryService.get(stranger, created.id()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("목록은 답변 여부를 함께 내린다")
    void listIncludesAnsweredFlag() {
        InquiryDetailResponse answered = inquiryService.create(parentId,
                new CreateInquiryRequest(InquiryCategory.ETC, "답변된 문의", "본문"));
        inquiryService.create(parentId,
                new CreateInquiryRequest(InquiryCategory.ETC, "대기 중 문의", "본문"));
        entityManager.flush();
        jdbcTemplate.update("""
                insert into inquiry_answers (inquiry_id, admin_name, content)
                values (?, '고객센터', '답변')
                """, answered.id());

        var list = inquiryService.list(parentId);
        assertThat(list.inquiries()).hasSize(2);
        assertThat(list.inquiries()).filteredOn(i -> i.id().equals(answered.id()))
                .allMatch(i -> i.answered());
    }
}
