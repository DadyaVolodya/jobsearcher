package ru.fsp.jobsearcher.matching;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class MatchingRanker {

    private MatchingRanker() {
    }

    public record RankedCandidate(
            String candidateId,
            String categoryCode,
            double score,
            List<String> explain
    ) {
    }

    public static double cosine(Map<String, Double> a, Map<String, Double> b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }
        Set<String> keys = new LinkedHashSet<>();
        keys.addAll(a.keySet());
        keys.addAll(b.keySet());
        double dot = 0;
        double na = 0;
        double nb = 0;
        for (String key : keys) {
            double va = a.getOrDefault(key, 0.0);
            double vb = b.getOrDefault(key, 0.0);
            dot += va * vb;
            na += va * va;
            nb += vb * vb;
        }
        if (na == 0 || nb == 0) {
            return 0.0;
        }
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    public static double stackOverlap(List<String> needStack, List<String> candidateStack) {
        if (needStack == null || needStack.isEmpty() || candidateStack == null || candidateStack.isEmpty()) {
            return 0.0;
        }
        Set<String> need = normalize(needStack);
        Set<String> cand = normalize(candidateStack);
        long hit = need.stream().filter(cand::contains).count();
        return (double) hit / need.size();
    }

    public static double fspBonus(int points) {
        if (points <= 0) {
            return 0.0;
        }
        return Math.min(0.2, points / 500.0);
    }

    public static double totalScore(
            Map<String, Double> needVector,
            Map<String, Double> candidateVector,
            List<String> needStack,
            List<String> candidateStack,
            double testScore,
            int fspPoints
    ) {
        double vector = cosine(needVector, candidateVector);
        double stack = stackOverlap(needStack, candidateStack);
        double test = Math.max(0.0, Math.min(1.0, testScore));
        return 0.45 * vector + 0.25 * stack + 0.20 * test + fspBonus(fspPoints);
    }

    public static List<String> explain(
            String specialization,
            String grade,
            List<String> needStack,
            List<String> candidateStack,
            double testScore,
            int fspPoints,
            double vectorScore
    ) {
        List<String> reasons = new ArrayList<>();
        reasons.add("Категория: " + specialization + " / " + grade);
        if (fspPoints > 0) {
            reasons.add("Есть подтверждённые достижения ФСП (+" + fspPoints + " баллов)");
        } else {
            reasons.add("История ФСП отсутствует — профиль оценён без спортивного бонуса");
        }
        if (vectorScore >= 0.6) {
            reasons.add("Высокое совпадение анкетных весов с потребностью");
        } else if (vectorScore >= 0.35) {
            reasons.add("Частичное совпадение анкетного профиля");
        }
        Set<String> overlap = normalize(needStack);
        overlap.retainAll(normalize(candidateStack));
        if (!overlap.isEmpty()) {
            reasons.add("Совпал стек: " + String.join(", ", overlap));
        }
        if (testScore >= 0.7) {
            reasons.add(String.format(Locale.ROOT, "Сильный результат теста (%.0f%%)", testScore * 100));
        }
        return reasons.stream().limit(4).toList();
    }

    public static List<RankedCandidate> sort(List<RankedCandidate> input) {
        return input.stream()
                .sorted(Comparator.comparingDouble(RankedCandidate::score).reversed())
                .toList();
    }

    private static Set<String> normalize(List<String> values) {
        Set<String> set = new LinkedHashSet<>();
        if (values == null) {
            return set;
        }
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                set.add(v.trim().toLowerCase(Locale.ROOT));
            }
        }
        return set;
    }
}
