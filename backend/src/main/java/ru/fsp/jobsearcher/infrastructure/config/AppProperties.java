package ru.fsp.jobsearcher.infrastructure.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Cors cors,
        Security security,
        int gradeChangeCooldownDays,
        Storage storage,
        Llm llm
) {
    public record Cors(String allowedOriginPatterns) {
    }

    public record Security(boolean permitAll) {
    }

    public record Storage(String type, String localRoot) {
    }

    public record Llm(Ollama ollama, Infereco infereco, long timeoutMs) {
        public record Ollama(String baseUrl, String apiKey, String model) {
        }

        public record Infereco(String baseUrl, String apiKey, List<String> models) {
            public Infereco {
                if (models == null) {
                    models = new ArrayList<>();
                }
            }
        }
    }
}
