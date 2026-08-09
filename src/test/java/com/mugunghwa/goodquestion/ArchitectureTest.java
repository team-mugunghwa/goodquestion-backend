package com.mugunghwa.goodquestion;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * 패키지 경계 규칙. 구조 결정을 문서가 아니라 빌드로 강제한다.
 *
 * <p>의존 방향: {@code learning → story → user}, {@code story·learning → ai}(단방향),
 * {@code home}은 도메인 위에 얹힌 조립 계층, {@code global}은 아무것도 모르는 공유 커널.
 */
@AnalyzeClasses(packages = "com.mugunghwa.goodquestion", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String ROOT = "com.mugunghwa.goodquestion.";
    private static final String AI = ROOT + "ai..";
    private static final String USER = ROOT + "user..";
    private static final String STORY = ROOT + "story..";
    private static final String LEARNING = ROOT + "learning..";
    private static final String HOME = ROOT + "home..";
    private static final String GLOBAL = ROOT + "global..";

    /** ai가 도메인을 역참조하면 무상태 어댑터가 아니게 되고 global.vocab의 존재 이유가 사라진다. */
    @ArchTest
    static final ArchRule ai_must_not_depend_on_domains =
            noClasses().that().resideInAPackage(AI)
                    .should().dependOnClassesThat().resideInAnyPackage(USER, STORY, LEARNING, HOME)
                    .because("ai는 무상태 어댑터다 — global.vocab enum과 자체 DTO로만 소통한다");

    /** 정책-02: 판정·종료·정답은 전부 서버가 담당하고 LLM에 위임하지 않는다. */
    @ArchTest
    static final ArchRule progression_engine_must_not_call_ai =
            noClasses().that().resideInAPackage(ROOT + "story.dialogue.engine..")
                    .should().dependOnClassesThat().resideInAnyPackage(AI)
                    .because("정책-02: 진행 판단·종료 결정은 순수 서버 규칙이며 LLM 호출을 섞지 않는다");

    @ArchTest
    static final ArchRule user_must_not_depend_on_upper_domains =
            noClasses().that().resideInAPackage(USER)
                    .should().dependOnClassesThat().resideInAnyPackage(STORY, LEARNING, AI, HOME)
                    .because("user는 의존 사슬의 최하단이다");

    @ArchTest
    static final ArchRule story_must_not_depend_on_learning_or_home =
            noClasses().that().resideInAPackage(STORY)
                    .should().dependOnClassesThat().resideInAnyPackage(LEARNING, HOME)
                    .because("의존 방향은 learning → story 단방향이다 — 역방향이 필요하면 이벤트로 뒤집는다");

    @ArchTest
    static final ArchRule learning_must_not_depend_on_home =
            noClasses().that().resideInAPackage(LEARNING)
                    .should().dependOnClassesThat().resideInAnyPackage(HOME)
                    .because("home은 도메인 위에 얹힌 조립 계층이므로 어떤 도메인도 의존하지 않는다");

    @ArchTest
    static final ArchRule global_must_not_depend_on_domains =
            noClasses().that().resideInAPackage(GLOBAL)
                    .should().dependOnClassesThat().resideInAnyPackage(USER, STORY, LEARNING, AI, HOME)
                    .because("global은 아무것도 모르는 공유 커널이다");

    /** 데이터-02: 콘텐츠는 읽기 전용 참조이며 수정은 복사 등록으로 처리한다. */
    @ArchTest
    static final ArchRule content_must_not_depend_on_runtime =
            noClasses().that().resideInAPackage(ROOT + "story.content..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            ROOT + "story.session..", ROOT + "story.dialogue..", ROOT + "story.mission..")
                    .because("정적 콘텐츠는 런타임 상태를 알지 못한다");

    @ArchTest
    static final ArchRule top_level_slices_must_be_free_of_cycles =
            slices().matching(ROOT + "(*)..").should().beFreeOfCycles();
}
