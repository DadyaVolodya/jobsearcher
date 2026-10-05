package ru.fsp.jobsearcher.infrastructure.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.fsp.jobsearcher.api.common.ApiException;
import ru.fsp.jobsearcher.api.common.ErrorCode;
import ru.fsp.jobsearcher.infrastructure.config.AppProperties;

@Slf4j
@Component
@RequiredArgsConstructor
public class LlmGateway {

    private final AppProperties props;
    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;

    public String chat(String systemPrompt, String userPrompt) {
        Exception last = null;
        try {
            String ollamaKey = props.llm().ollama().apiKey();
            if (ollamaKey != null && !ollamaKey.isBlank()) {
                return chatOllama(systemPrompt, userPrompt);
            }
        } catch (Exception ex) {
            last = ex;
            log.warn("Ollama failed, trying Infereco: {}", ex.getMessage());
        }
        List<String> models = props.llm().infereco().models();
        for (String model : models) {
            try {
                return chatInfereco(model, systemPrompt, userPrompt);
            } catch (Exception ex) {
                last = ex;
                log.warn("Infereco model {} failed: {}", model, ex.getMessage());
            }
        }
        throw new ApiException(ErrorCode.LLM_UNAVAILABLE, org.springframework.http.HttpStatus.BAD_GATEWAY,
                "LLM unavailable: " + (last == null ? "no providers configured" : last.getMessage()));
    }

    private String chatOllama(String systemPrompt, String userPrompt) throws Exception {
        RestClient client = restClientBuilder.baseUrl(props.llm().ollama().baseUrl()).build();
        Map<String, Object> body = Map.of(
                "model", props.llm().ollama().model(),
                "stream", false,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                )
        );
        String raw = client.post()
                .uri("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + props.llm().ollama().apiKey())
                .body(body)
                .retrieve()
                .body(String.class);
        JsonNode root = objectMapper.readTree(raw);
        JsonNode content = root.path("message").path("content");
        if (content.isMissingNode() || content.asText().isBlank()) {
            throw new IllegalStateException("Empty Ollama response");
        }
        return content.asText();
    }

    private String chatInfereco(String model, String systemPrompt, String userPrompt) throws Exception {
        String apiKey = props.llm().infereco().apiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("INFERECO_API_KEY is empty");
        }
        RestClient client = restClientBuilder.baseUrl(props.llm().infereco().baseUrl()).build();
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                )
        );
        String raw = client.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + apiKey)
                .body(body)
                .retrieve()
                .body(String.class);
        JsonNode root = objectMapper.readTree(raw);
        JsonNode content = root.path("choices").path(0).path("message").path("content");
        if (content.isMissingNode() || content.asText().isBlank()) {
            throw new IllegalStateException("Empty Infereco response");
        }
        return content.asText();
    }
}
