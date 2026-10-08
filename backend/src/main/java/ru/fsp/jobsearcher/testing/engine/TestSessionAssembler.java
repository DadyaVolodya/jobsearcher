package ru.fsp.jobsearcher.testing.engine;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.zip.CRC32;
import ru.fsp.jobsearcher.domain.entity.TestItem;
import ru.fsp.jobsearcher.domain.enums.Grade;

public final class TestSessionAssembler {

    private TestSessionAssembler() {
    }

    public record ScoreBreakdown(
            double overall,
            double juniorFloor,
            Map<String, Double> bySection
    ) {
    }

    public static long seedOf(UUID candidateId, UUID attemptId) {
        CRC32 crc = new CRC32();
        crc.update((candidateId + ":" + attemptId).getBytes(StandardCharsets.UTF_8));
        return crc.getValue();
    }

    public static List<Map<String, Object>> assemble(List<TestItem> bank, long seed, int limit) {
        Random random = new Random(seed);
        List<TestItem> shuffled = new ArrayList<>(bank);
        Collections.shuffle(shuffled, random);
        // Prefer mix A/B/C
        List<TestItem> ordered = new ArrayList<>();
        for (String section : List.of("A", "B", "C")) {
            shuffled.stream().filter(i -> section.equalsIgnoreCase(nullToB(i.getSection()))).forEach(ordered::add);
        }
        shuffled.stream().filter(i -> !ordered.contains(i)).forEach(ordered::add);

        List<Map<String, Object>> items = new ArrayList<>();
        int take = Math.min(limit, ordered.size());
        for (int i = 0; i < take; i++) {
            TestItem item = ordered.get(i);
            Map<String, Integer> params = rollParams(item.getParamSchema(), random);
            Map<String, Object> variant = pickVariant(item, random);
            String prompt = renderPrompt(item, params, variant);
            List<Map<String, Object>> options = buildOptions(item, params, random);

            Map<String, Object> dto = new HashMap<>();
            dto.put("itemId", item.getId().toString());
            dto.put("code", item.getCode());
            dto.put("questionType", item.getQuestionType().name());
            dto.put("section", nullToB(item.getSection()));
            dto.put("gradeLevel", item.getGrade().name());
            dto.put("prompt", prompt);
            dto.put("antiAiPrompt", item.getAntiAiPrompt());
            dto.put("captchaStyle", item.isCaptchaStyle());
            dto.put("copyPasteBlocked", true);
            dto.put("options", options);
            dto.put("weight", item.getWeight());
            dto.put("variantId", variant.getOrDefault("id", "A"));
            dto.put("params", params);
            dto.put("expected", expectedAnswer(item, params));
            items.add(dto);
        }
        return items;
    }

    public static ScoreBreakdown gradeAnswers(
            List<Map<String, Object>> sessionItems,
            Map<String, Object> answers,
            Map<String, TestItem> byId
    ) {
        double earned = 0;
        double total = 0;
        double junEarned = 0;
        double junTotal = 0;
        Map<String, Double> secEarned = new LinkedHashMap<>();
        Map<String, Double> secTotal = new LinkedHashMap<>();

        for (Map<String, Object> sessionItem : sessionItems) {
            String itemId = String.valueOf(sessionItem.get("itemId"));
            TestItem item = byId.get(itemId);
            if (item == null) {
                continue;
            }
            String section = String.valueOf(sessionItem.getOrDefault("section", "B"));
            double w = item.getWeight();
            total += w;
            secTotal.merge(section, w, Double::sum);
            boolean juniorFloor = item.getGrade() == Grade.JUNIOR || item.getGrade() == Grade.INTERN
                    || "A".equalsIgnoreCase(section);
            if (juniorFloor) {
                junTotal += w;
            }

            Object answer = answers.get(item.getCode());
            if (answer == null) {
                continue;
            }
            double credit = scoreOne(sessionItem, item, answer);
            earned += credit;
            secEarned.merge(section, credit, Double::sum);
            if (juniorFloor) {
                junEarned += credit;
            }
        }

        Map<String, Double> bySection = new LinkedHashMap<>();
        for (String s : List.of("A", "B", "C")) {
            double t = secTotal.getOrDefault(s, 0.0);
            bySection.put(s, t == 0 ? 0.0 : secEarned.getOrDefault(s, 0.0) / t);
        }
        double overall = total == 0 ? 0.0 : earned / total;
        double juniorFloor = junTotal == 0 ? overall : junEarned / junTotal;
        return new ScoreBreakdown(overall, juniorFloor, bySection);
    }

    private static double scoreOne(Map<String, Object> sessionItem, TestItem item, Object answer) {
        Object expected = sessionItem.get("expected");
        if (expected != null && !String.valueOf(expected).isBlank()) {
            return String.valueOf(expected).equalsIgnoreCase(String.valueOf(answer).trim())
                    ? item.getWeight() : 0.0;
        }
        if (item.getQuestionType().name().contains("CHOICE")) {
            List<?> correct = (List<?>) item.getCorrectAnswer().getOrDefault("correctCodes", List.of());
            if (correct.stream().anyMatch(c -> String.valueOf(c).equals(String.valueOf(answer)))) {
                return item.getWeight();
            }
            return 0.0;
        }
        String text = String.valueOf(answer).toLowerCase(Locale.ROOT);
        String rubric = item.getRubric() == null ? "" : item.getRubric().toLowerCase(Locale.ROOT);
        long hits = java.util.Arrays.stream(rubric.split("[^a-zа-я0-9+]+"))
                .filter(t -> t.length() > 3)
                .filter(text::contains)
                .count();
        if (hits >= 2 || (hits >= 1 && text.length() > 40)) {
            return item.getWeight() * 0.8;
        }
        if (text.length() > 20) {
            return item.getWeight() * 0.4;
        }
        return 0.0;
    }

    private static Map<String, Integer> rollParams(Map<String, Object> schema, Random random) {
        Map<String, Integer> params = new HashMap<>();
        if (schema == null || schema.isEmpty()) {
            return params;
        }
        for (Map.Entry<String, Object> e : schema.entrySet()) {
            if (e.getValue() instanceof Map<?, ?> bounds) {
                int min = toInt(bounds.get("min"), 1);
                int max = toInt(bounds.get("max"), min);
                params.put(e.getKey(), min + random.nextInt(Math.max(1, max - min + 1)));
            }
        }
        return params;
    }

    private static String renderPrompt(TestItem item, Map<String, Integer> params, Map<String, Object> variant) {
        String prompt = item.getPromptTemplate()
                .replace("{{variant}}", String.valueOf(variant.getOrDefault("id", "A")))
                .replace("{{antiAi}}", item.getAntiAiPrompt() == null ? "" : item.getAntiAiPrompt());
        for (Map.Entry<String, Integer> p : params.entrySet()) {
            prompt = prompt.replace("{{" + p.getKey() + "}}", String.valueOf(p.getValue()));
        }
        if (variant.containsKey("context")) {
            prompt = prompt + " [" + variant.get("context") + "]";
        }
        if (item.isCaptchaStyle()) {
            prompt = "[CAPTCHA/рукописный стиль - CV/OCR враждебно] " + prompt;
        }
        return prompt;
    }

    private static List<Map<String, Object>> buildOptions(TestItem item, Map<String, Integer> params, Random random) {
        String mode = item.getCorrectAnswer() == null ? null : String.valueOf(item.getCorrectAnswer().get("mode"));
        if (mode != null && mode.startsWith("PARAM_")) {
            int correct = Integer.parseInt(expectedAnswer(item, params));
            List<Integer> values = new ArrayList<>(List.of(correct, correct + 1, correct - 1, correct + 2));
            Collections.shuffle(values, random);
            List<Map<String, Object>> options = new ArrayList<>();
            for (Integer v : values) {
                options.add(Map.of("code", String.valueOf(v), "label", String.valueOf(v)));
            }
            return options;
        }
        return shuffleOptions(item.getOptionsPool(), random).stream().map(TestSessionAssembler::publicOption).toList();
    }

    private static String expectedAnswer(TestItem item, Map<String, Integer> params) {
        if (item.getCorrectAnswer() == null) {
            return null;
        }
        String mode = String.valueOf(item.getCorrectAnswer().getOrDefault("mode", ""));
        return switch (mode) {
            case "PARAM_SUM" -> String.valueOf(params.getOrDefault("a", 0) + params.getOrDefault("b", 0));
            case "PARAM_N" -> String.valueOf(params.getOrDefault("n", 0));
            case "PARAM_BOX" -> String.valueOf(100 + 2 * params.getOrDefault("a", 0) + 2 * params.getOrDefault("b", 0));
            default -> null;
        };
    }

    private static Map<String, Object> pickVariant(TestItem item, Random random) {
        List<Map<String, Object>> variants = item.getVariants();
        if (variants == null || variants.isEmpty()) {
            return Map.of("id", "A");
        }
        return variants.get(random.nextInt(variants.size()));
    }

    private static List<Map<String, Object>> shuffleOptions(List<Map<String, Object>> pool, Random random) {
        if (pool == null || pool.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> copy = new ArrayList<>(pool);
        Collections.shuffle(copy, random);
        return copy;
    }

    private static Map<String, Object> publicOption(Map<String, Object> option) {
        Map<String, Object> pub = new HashMap<>();
        pub.put("code", option.get("code"));
        pub.put("label", option.get("label"));
        return pub;
    }

    private static String nullToB(String section) {
        return section == null || section.isBlank() ? "B" : section;
    }

    private static int toInt(Object v, int def) {
        if (v instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (Exception e) {
            return def;
        }
    }
}
