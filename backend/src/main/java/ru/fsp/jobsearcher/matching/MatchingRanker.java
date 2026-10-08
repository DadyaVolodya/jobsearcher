package ru.fsp.jobsearcher.matching;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import ru.fsp.jobsearcher.domain.enums.Grade;

public final class MatchingRanker {

    private MatchingRanker() {
    }

    public record RankedCandidate(
            String candidateId,
            String categoryCode,
            double score,
            int fspPoints,
            String fspGrade,
            boolean fspLinked,
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

    /**
     * Участники ФСП с грейдом/достижениями заметно выше в выдаче.
     */
    public static double fspBonus(int points, Grade fspGrade, Grade needGrade) {
        boolean linked = points > 0 || fspGrade != null;
        if (!linked) {
            return 0.0;
        }
        double bonus = 0.18;
        bonus += Math.min(0.22, Math.max(0, points) / 400.0);
        if (fspGrade != null && needGrade != null) {
            if (fspGrade.level() >= needGrade.level()) {
                bonus += 0.12;
            } else {
                bonus += 0.05;
            }
        } else if (fspGrade != null) {
            bonus += 0.08;
        }
        return Math.min(0.45, bonus);
    }

    public static double fspBonus(int points) {
        return fspBonus(points, null, null);
    }

    public static double totalScore(
            Map<String, Double> needVector,
            Map<String, Double> candidateVector,
            List<String> needStack,
            List<String> candidateStack,
            double testScore,
            int fspPoints,
            Grade fspGrade,
            Grade needGrade
    ) {
        double vector = cosine(needVector, candidateVector);
        double stack = stackOverlap(needStack, candidateStack);
        double test = Math.max(0.0, Math.min(1.0, testScore));
        return 0.40 * vector + 0.22 * stack + 0.18 * test + fspBonus(fspPoints, fspGrade, needGrade);
    }

    public static double totalScore(
            Map<String, Double> needVector,
            Map<String, Double> candidateVector,
            List<String> needStack,
            List<String> candidateStack,
            double testScore,
            int fspPoints
    ) {
        return totalScore(needVector, candidateVector, needStack, candidateStack, testScore, fspPoints, null, null);
    }

    public static List<String> explain(
            String specialization,
            String grade,
            List<String> needStack,
            List<String> candidateStack,
            double testScore,
            int fspPoints,
            Grade fspGrade,
            double vectorScore
    ) {
        List<String> reasons = new ArrayList<>();
        if (fspPoints > 0 || fspGrade != null) {
            String gradePart = fspGrade == null ? "" : (", грейд ФСП " + fspGrade.name());
            reasons.add("Приоритет ФСП: достижения +" + fspPoints + " баллов" + gradePart);
        } else {
            reasons.add("История ФСП отсутствует — профиль без спортивного приоритета");
        }
        reasons.add("Категория: " + specialization + " / " + grade);
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
        return reasons.stream().limit(5).toList();
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
        return explain(specialization, grade, needStack, candidateStack, testScore, fspPoints, null, vectorScore);
    }

    public static List<RankedCandidate> sort(List<RankedCandidate> input) {
        return input.stream()
                .sorted(Comparator.comparingDouble(RankedCandidate::score).reversed()
                        .thenComparing(Comparator.comparingInt(RankedCandidate::fspPoints).reversed())
                        .thenComparing(Comparator.comparing(RankedCandidate::fspLinked).reversed()))
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
