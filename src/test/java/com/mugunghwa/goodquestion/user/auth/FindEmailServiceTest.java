package com.mugunghwa.goodquestion.user.auth;

import com.mugunghwa.goodquestion.support.IntegrationTest;
import com.mugunghwa.goodquestion.user.auth.dto.FindEmailRequest;
import com.mugunghwa.goodquestion.user.auth.dto.FindEmailResponse;
import com.mugunghwa.goodquestion.user.auth.dto.SignUpRequest;
import com.mugunghwa.goodquestion.user.child.Child;
import com.mugunghwa.goodquestion.user.child.ChildRepository;
import com.mugunghwa.goodquestion.user.parent.Parent;
import com.mugunghwa.goodquestion.user.parent.ParentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 이메일(ID) 찾기 (계정-07).
 *
 * <p>핵심은 자녀가 있는 계정이 이름만으로는 절대 나오지 않는다는 것 — 그 경로는
 * 자녀 정보까지 맞혀야만 열린다.
 */
@IntegrationTest
@Transactional
class FindEmailServiceTest {

    private static final String PASSWORD = "password123";
    private static final String IP = "127.0.0.1";

    @Autowired
    private AuthService authService;

    @Autowired
    private FindEmailService findEmailService;

    @Autowired
    private ParentRepository parentRepository;

    @Autowired
    private ChildRepository childRepository;

    private Parent 가입한다(String email, String parentName) {
        authService.signUp(new SignUpRequest(email, PASSWORD, parentName), IP);
        return parentRepository.findByEmail(email).orElseThrow();
    }

    private void 아이를_등록한다(Parent parent, String childName, short birthYear) {
        childRepository.save(Child.builder().parent(parent).name(childName).birthYear(birthYear).build());
    }

    @Test
    void 자녀_정보까지_정확히_일치하면_마스킹된_이메일을_돌려준다() {
        Parent parent = 가입한다("child-match@test.com", "김보호");
        아이를_등록한다(parent, "김아이", (short) 2018);

        FindEmailResponse response = findEmailService.find(
                new FindEmailRequest("김보호", "김아이", (short) 2018));

        assertThat(response.emails()).containsExactly("ch***@test.com");
    }

    @Test
    void 자녀_정보가_틀리면_빈_리스트() {
        Parent parent = 가입한다("child-mismatch@test.com", "박보호");
        아이를_등록한다(parent, "박아이", (short) 2019);

        FindEmailResponse 다른이름 = findEmailService.find(
                new FindEmailRequest("박보호", "다른아이", (short) 2019));
        FindEmailResponse 다른생년 = findEmailService.find(
                new FindEmailRequest("박보호", "박아이", (short) 2020));

        assertThat(다른이름.emails()).isEmpty();
        assertThat(다른생년.emails()).isEmpty();
    }

    @Test
    void 자녀_없는_계정은_이름만으로_찾을_수_있다() {
        가입한다("no-child@test.com", "최보호");

        FindEmailResponse response = findEmailService.find(
                new FindEmailRequest("최보호", null, null));

        assertThat(response.emails()).containsExactly("no***@test.com");
    }

    @Test
    void 자녀_있는_계정은_이름만으로는_절대_나오지_않는다() {
        Parent parent = 가입한다("has-child@test.com", "이보호");
        아이를_등록한다(parent, "이아이", (short) 2017);

        FindEmailResponse response = findEmailService.find(
                new FindEmailRequest("이보호", null, null));

        assertThat(response.emails()).isEmpty();
    }

    @Test
    void 동명이인_두_계정이_매치되면_둘_다_반환한다() {
        가입한다("twin-a@test.com", "정보호");
        가입한다("twin-b@test.com", "정보호");

        FindEmailResponse response = findEmailService.find(
                new FindEmailRequest("정보호", null, null));

        assertThat(response.emails()).containsExactlyInAnyOrder("tw***@test.com", "tw***@test.com");
        assertThat(response.emails()).hasSize(2);
    }
}
