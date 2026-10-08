package ru.fsp.jobsearcher.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import ru.fsp.jobsearcher.domain.enums.Grade;
import ru.fsp.jobsearcher.matching.MatchingRanker;

class MatchingRankerTest {

    @Test
    void cosineIdenticalIsOne() {
        Map<String, Double> v = Map.of("a", 1.0, "b", 0.5);
        assertThat(MatchingRanker.cosine(v, v)).isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-9));
    }

    @Test
    void stackOverlapPartial() {
        assertThat(MatchingRanker.stackOverlap(List.of("Java", "Go"), List.of("java", "React")))
                .isEqualTo(0.5);
    }

    @Test
    void explainPutsFspFirstWhenPresent() {
        List<String> explain = MatchingRanker.explain(
                "BACKEND", "MIDDLE", List.of("Java"), List.of("Java"), 0.8, 120, Grade.SENIOR, 0.7);
        assertThat(explain.getFirst()).contains("Приоритет ФСП");
        assertThat(explain.getFirst()).contains("SENIOR");
    }

    @Test
    void explainContainsFspAbsence() {
        List<String> explain = MatchingRanker.explain("BACKEND", "MIDDLE", List.of("Java"), List.of("Java"), 0.8, 0, 0.7);
        assertThat(explain.stream().anyMatch(s -> s.contains("ФСП"))).isTrue();
    }

    @Test
    void totalScoreIncreasesWithFsp() {
        Map<String, Double> v = Map.of("a", 1.0);
        double without = MatchingRanker.totalScore(v, v, List.of("Java"), List.of("Java"), 0.8, 0);
        double with = MatchingRanker.totalScore(v, v, List.of("Java"), List.of("Java"), 0.8, 100, Grade.MIDDLE, Grade.MIDDLE);
        assertThat(with).isGreaterThan(without);
        assertThat(with - without).isGreaterThan(0.2);
    }

    @Test
    void sortPutsFspCandidateAboveEqualBase() {
        var low = new MatchingRanker.RankedCandidate("a", "IT:BACKEND:JUNIOR", 0.5, 0, null, false, List.of());
        var high = new MatchingRanker.RankedCandidate("b", "IT:BACKEND:JUNIOR", 0.55, 200, "SENIOR", true, List.of());
        assertThat(MatchingRanker.sort(List.of(low, high)).getFirst().candidateId()).isEqualTo("b");
    }
}
