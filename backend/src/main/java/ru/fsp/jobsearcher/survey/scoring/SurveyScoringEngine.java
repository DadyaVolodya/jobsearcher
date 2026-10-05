package ru.fsp.jobsearcher.survey.scoring;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ru.fsp.jobsearcher.domain.entity.SurveyQuestion;

public final class SurveyScoringEngine {

    private SurveyScoringEngine() {
    }

    public static Map<String, Double> score(List<SurveyQuestion> questions, Map<String, Object> answers) {
        Map<String, Double> weightedSum = new HashMap<>();
        Map<String, Double> weightSum = new HashMap<>();

        for (SurveyQuestion q : questions) {
            Object answer = answers.get(q.getCode());
            if (answer == null) {
                continue;
            }
            double optionScore = resolveOptionScore(q, String.valueOf(answer));
            weightedSum.merge(q.getScaleKey(), optionScore * q.getWeight(), Double::sum);
            weightSum.merge(q.getScaleKey(), q.getWeight(), Double::sum);
        }

        Map<String, Double> vector = new HashMap<>();
        for (Map.Entry<String, Double> e : weightedSum.entrySet()) {
            double w = weightSum.getOrDefault(e.getKey(), 1.0);
            vector.put(e.getKey(), w == 0 ? 0.0 : e.getValue() / w);
        }
        return vector;
    }

    private static double resolveOptionScore(SurveyQuestion question, String answerCode) {
        for (Map<String, Object> option : question.getOptions()) {
            if (answerCode.equals(String.valueOf(option.get("code")))) {
                Object score = option.get("score");
                if (score instanceof Number n) {
                    return n.doubleValue();
                }
            }
        }
        return 0.0;
    }
}
