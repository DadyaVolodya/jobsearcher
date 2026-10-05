package ru.fsp.jobsearcher.unit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import ru.fsp.jobsearcher.api.common.ApiException;
import ru.fsp.jobsearcher.infrastructure.config.AppProperties;
import ru.fsp.jobsearcher.infrastructure.llm.LlmGateway;

class LlmGatewayFailoverTest {

    @Test
    void throwsWhenNoProvidersConfigured() {
        AppProperties props = new AppProperties(
                new AppProperties.Cors("*"),
                new AppProperties.Security(true),
                90,
                new AppProperties.Storage("local", "/tmp"),
                new AppProperties.Llm(
                        new AppProperties.Llm.Ollama("http://localhost", "", "m"),
                        new AppProperties.Llm.Infereco("http://localhost", "", List.of()),
                        1000
                )
        );
        RestClient.Builder builder = mock(RestClient.Builder.class);
        when(builder.baseUrl(any())).thenReturn(builder);
        LlmGateway gateway = new LlmGateway(props, builder, new ObjectMapper());
        assertThatThrownBy(() -> gateway.chat("s", "u")).isInstanceOf(ApiException.class);
    }
}
