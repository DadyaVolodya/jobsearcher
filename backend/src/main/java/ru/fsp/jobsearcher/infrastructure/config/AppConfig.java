package ru.fsp.jobsearcher.infrastructure.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import ru.fsp.jobsearcher.application.policy.GradeChangePolicy;

@Configuration
public class AppConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    GradeChangePolicy gradeChangePolicy(AppProperties props, Clock clock) {
        return new GradeChangePolicy(props.gradeChangeCooldownDays(), clock);
    }

    @Bean
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
