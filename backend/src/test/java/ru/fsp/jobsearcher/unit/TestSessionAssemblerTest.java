package ru.fsp.jobsearcher.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import ru.fsp.jobsearcher.domain.entity.TestItem;
import ru.fsp.jobsearcher.domain.enums.Grade;
import ru.fsp.jobsearcher.domain.enums.QuestionType;
import ru.fsp.jobsearcher.testing.engine.TestSessionAssembler;

class TestSessionAssemblerTest {

    @Test
    void seedIsDeterministic() {
        UUID c = UUID.randomUUID();
        UUID a = UUID.randomUUID();
        assertThat(TestSessionAssembler.seedOf(c, a)).isEqualTo(TestSessionAssembler.seedOf(c, a));
    }

    @Test
    void differentAttemptsDifferentSeeds() {
        UUID c = UUID.randomUUID();
        assertThat(TestSessionAssembler.seedOf(c, UUID.randomUUID()))
                .isNotEqualTo(TestSessionAssembler.seedOf(c, UUID.randomUUID()));
    }

    @Test
    void assembleHidesCorrectFlagsAndSetsSections() {
        TestItem item = item();
        List<Map<String, Object>> session = TestSessionAssembler.assemble(List.of(item), 42L, 1);
        assertThat(session.getFirst().get("section")).isEqualTo("A");
        assertThat(session.getFirst().get("copyPasteBlocked")).isEqualTo(true);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> options = (List<Map<String, Object>>) session.getFirst().get("options");
        assertThat(options).isNotEmpty();
        assertThat(options.getFirst()).doesNotContainKey("correct");
    }

    @Test
    void gradesChoiceAnswers() {
        TestItem item = item();
        List<Map<String, Object>> session = TestSessionAssembler.assemble(List.of(item), 1L, 1);
        Map<String, TestItem> byId = Map.of(item.getId().toString(), item);
        var good = TestSessionAssembler.gradeAnswers(session, Map.of(item.getCode(), "YES"), byId);
        var bad = TestSessionAssembler.gradeAnswers(session, Map.of(item.getCode(), "NO"), byId);
        assertThat(good.overall()).isEqualTo(1.0);
        assertThat(bad.overall()).isEqualTo(0.0);
    }

    private static TestItem item() {
        TestItem t = new TestItem();
        t.setId(UUID.randomUUID());
        t.setCode("T1");
        t.setIndustryCode("IT");
        t.setSpecializationCode("BACKEND");
        t.setGrade(Grade.JUNIOR);
        t.setQuestionType(QuestionType.SINGLE_CHOICE);
        t.setPromptTemplate("Q {{variant}}");
        t.setVariants(List.of(Map.of("id", "A")));
        t.setOptionsPool(List.of(
                Map.of("code", "YES", "label", "Yes", "correct", true),
                Map.of("code", "NO", "label", "No", "correct", false)
        ));
        Map<String, Object> correct = new HashMap<>();
        correct.put("correctCodes", List.of("YES"));
        t.setCorrectAnswer(correct);
        t.setWeight(1.0);
        t.setActive(true);
        t.setSection("A");
        t.setAntiAiPrompt("no ai");
        t.setCaptchaStyle(false);
        t.setParamSchema(Map.of());
        return t;
    }
}
