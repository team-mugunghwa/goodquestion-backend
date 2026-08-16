package com.mugunghwa.goodquestion.user.parent;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import com.mugunghwa.goodquestion.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTest
@Transactional
class ParentServiceTest {

    /** 시드의 이메일 계정. 비밀번호는 demo1234! 이다. */
    private static final UUID LOCAL_PARENT_ID =
            UUID.fromString("99999999-9999-9999-9999-000000000001");

    /** 시드의 카카오 계정. passwordHash가 없다. */
    private static final UUID SOCIAL_PARENT_ID =
            UUID.fromString("99999999-9999-9999-9999-000000000002");

    @Autowired
    private ParentService parentService;

    @Test
    void 비밀번호가_맞으면_통과한다() {
        assertThatCode(() -> parentService.verifyPassword(LOCAL_PARENT_ID, "demo1234!"))
                .doesNotThrowAnyException();
    }

    @Test
    void 비밀번호가_틀리면_거절한다() {
        assertThatThrownBy(() -> parentService.verifyPassword(LOCAL_PARENT_ID, "wrong1234!"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    void 소셜_계정은_비밀번호로_확인할_수_없다() {
        assertThatThrownBy(() -> parentService.verifyPassword(SOCIAL_PARENT_ID, "아무거나"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }
}