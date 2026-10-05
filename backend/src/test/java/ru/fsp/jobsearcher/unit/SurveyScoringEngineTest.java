package ru.fsp.jobsearcher.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import ru.fsp.jobsearcher.domain.entity.SurveyQuestion;
import ru.fsp.jobsearcher.domain.enums.QuestionType;
import ru.fsp.jobsearcher.survey.scoring.SurveyScoringEngine;

class SurveyScoringEngineTest {

    @Test
    void scoresWeightedScales() {
        SurveyQuestion q1 = question("Q1", "architecture", 2.0, "4", 0.8);
        SurveyQuestion q2 = question("Q2", "architecture", 1.0, "5", 1.0);
        Map<String, Double> vector = SurveyScoringEngine.score(
                List.of(q1, q2),
                Map.of("Q1", "4", "Q2", "5")
        );
        assertThat(vector.get("architecture")).isEqualTo((0.8 * 2 + 1.0 * 1) / 3.0);
    }

    private static SurveyQuestion question(String code, String scale, double weight, String opt, double score) {
        SurveyQuestion q = new SurveyQuestion();
        q.setId(UUID.randomUUID());
        q.setQuestionnaireId(UUID.randomUUID());
        q.setCode(code);
        q.setText("t");
        q.setQuestionType(QuestionType.SCALE);
        q.setWeight(weight);
        q.setScaleKey(scale);
        q.setOptions(List.of(Map.of("code", opt, "label", opt, "score", score)));
        return q;
    }
}
