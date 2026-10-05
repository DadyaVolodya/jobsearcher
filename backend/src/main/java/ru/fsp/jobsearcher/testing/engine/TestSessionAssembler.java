package ru.fsp.jobsearcher.testing.engine;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.zip.CRC32;
import ru.fsp.jobsearcher.domain.entity.TestItem;

public final class TestSessionAssembler {

    private TestSessionAssembler() {
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
        List<Map<String, Object>> items = new ArrayList<>();
        int take = Math.min(limit, shuffled.size());
        for (int i = 0; i < take; i++) {
            TestItem item = shuffled.get(i);
            Map<String, Object> variant = pickVariant(item, random);
            List<Map<String, Object>> options = shuffleOptions(item.getOptionsPool(), random);
            String prompt = item.getPromptTemplate()
                    .replace("{{variant}}", String.valueOf(variant.getOrDefault("id", "A")));
            if (variant.containsKey("context")) {
                prompt = prompt + " [" + variant.get("context") + "]";
            } else if (variant.containsKey("hint")) {
                prompt = prompt + " [" + variant.get("hint") + "]";
            }
            Map<String, Object> dto = new HashMap<>();
            dto.put("itemId", item.getId().toString());
            dto.put("code", item.getCode());
            dto.put("questionType", item.getQuestionType().name());
            dto.put("prompt", prompt);
            dto.put("options", options.stream().map(TestSessionAssembler::publicOption).toList());
            dto.put("weight", item.getWeight());
            dto.put("variantId", variant.getOrDefault("id", "A"));
            items.add(dto);
        }
        return items;
    }

    public static double gradeAnswers(
            List<Map<String, Object>> sessionItems,
            Map<String, Object> answers,
            Map<String, TestItem> byId
    ) {
        double earned = 0;
        double total = 0;
        for (Map<String, Object> sessionItem : sessionItems) {
            String itemId = String.valueOf(sessionItem.get("itemId"));
            TestItem item = byId.get(itemId);
            if (item == null) {
                continue;
            }
            total += item.getWeight();
            Object answer = answers.get(item.getCode());
            if (answer == null) {
                continue;
            }
            if (item.getQuestionType().name().contains("CHOICE")) {
                List<?> correct = (List<?>) item.getCorrectAnswer().getOrDefault("correctCodes", List.of());
                if (correct.stream().anyMatch(c -> String.valueOf(c).equals(String.valueOf(answer)))) {
                    earned += item.getWeight();
                }
            } else {
                String text = String.valueOf(answer).toLowerCase();
                String rubric = item.getRubric() == null ? "" : item.getRubric().toLowerCase();
                long hits = java.util.Arrays.stream(rubric.split("[^a-zа-я0-9+]+"))
                        .filter(t -> t.length() > 3)
                        .filter(text::contains)
                        .count();
                if (hits >= 2 || (hits >= 1 && text.length() > 40)) {
                    earned += item.getWeight() * 0.8;
                } else if (text.length() > 20) {
                    earned += item.getWeight() * 0.4;
                }
            }
        }
        return total == 0 ? 0.0 : earned / total;
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
}
