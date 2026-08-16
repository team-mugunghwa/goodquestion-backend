package com.mugunghwa.goodquestion.learning.activity;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import com.mugunghwa.goodquestion.learning.activity.dto.ChildActivityResponse;
import com.mugunghwa.goodquestion.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static com.mugunghwa.goodquestion.learning.reward.RewardFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTest
@Transactional
class ChildActivityServiceTest {

    @Autowired
    private ChildActivityService activityService;

    @Test
    void 같은_이야기를_여러_번_완주해도_한_편으로_센다() {
        ChildActivityResponse response = activityService.getActivity(PARENT_ID, CHILD_ID);

        assertThat(response.completedStories()).isEqualTo(1);
        assertThat(response.stardust()).isNotNegative();
    }

    @Test
    void 남의_아이는_볼_수_없다() {
        assertThatThrownBy(() -> activityService.getActivity(OTHER_PARENT_ID, CHILD_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }
}