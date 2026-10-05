package ru.fsp.jobsearcher.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import ru.fsp.jobsearcher.api.common.ApiException;
import ru.fsp.jobsearcher.application.policy.GradeChangePolicy;
import ru.fsp.jobsearcher.domain.enums.Grade;

class GradeChangePolicyTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-05T00:00:00Z"), ZoneOffset.UTC);
    private final GradeChangePolicy policy = new GradeChangePolicy(90, clock);

    @Test
    void cooldownBlocksRecentChange() {
        assertThatThrownBy(() -> policy.assertCanChange(Instant.parse("2026-09-01T00:00:00Z")))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void cooldownAllowsOldChange() {
        policy.assertCanChange(Instant.parse("2026-01-01T00:00:00Z"));
    }

    @Test
    void failedTestGoesLower() {
        assertThat(policy.resolveAfterTest(Grade.MIDDLE, false, 0.2)).isEqualTo(Grade.JUNIOR);
    }

    @Test
    void excellentPassCanRaise() {
        assertThat(policy.resolveAfterTest(Grade.JUNIOR, true, 0.95)).isEqualTo(Grade.MIDDLE);
    }
}
