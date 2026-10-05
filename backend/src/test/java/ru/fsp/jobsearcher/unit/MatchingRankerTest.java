package ru.fsp.jobsearcher.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
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
    void explainContainsFspAbsence() {
        List<String> explain = MatchingRanker.explain("BACKEND", "MIDDLE", List.of("Java"), List.of("Java"), 0.8, 0, 0.7);
        assertThat(explain.stream().anyMatch(s -> s.contains("ФСП"))).isTrue();
    }

    @Test
    void totalScoreIncreasesWithFsp() {
        Map<String, Double> v = Map.of("a", 1.0);
        double without = MatchingRanker.totalScore(v, v, List.of("Java"), List.of("Java"), 0.8, 0);
        double with = MatchingRanker.totalScore(v, v, List.of("Java"), List.of("Java"), 0.8, 100);
        assertThat(with).isGreaterThan(without);
    }
}
