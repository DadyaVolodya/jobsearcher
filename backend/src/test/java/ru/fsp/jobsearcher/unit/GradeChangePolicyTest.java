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
    private final GradeChangePolicy policy = new GradeChangePolicy(30, clock);

    @Test
    void cooldownBlocksRecentChange() {
        assertThatThrownBy(() -> policy.assertCanChange(Instant.parse("2026-09-20T00:00:00Z")))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void cooldownAllowsOldChange() {
        policy.assertCanChange(Instant.parse("2026-08-01T00:00:00Z"));
    }

    @Test
    void passKeepsTarget() {
        GradeChangePolicy.Outcome o = policy.resolve(Grade.SENIOR, Grade.MIDDLE, 0.7, 0.6);
        assertThat(o.assigned()).isEqualTo(Grade.SENIOR);
        assertThat(o.confirmed()).isTrue();
        assertThat(o.suggestedRetry()).isNull();
    }

    @Test
    void failDoesNotDemotePrevious() {
        GradeChangePolicy.Outcome o = policy.resolve(Grade.SENIOR, Grade.MIDDLE, 0.3, 0.6);
        assertThat(o.assigned()).isEqualTo(Grade.MIDDLE);
        assertThat(o.confirmed()).isFalse();
        assertThat(o.suggestedRetry()).isEqualTo(Grade.MIDDLE);
    }

    @Test
    void weakFloorSuggestsJuniorWithoutDemotion() {
        GradeChangePolicy.Outcome o = policy.resolve(Grade.SENIOR, Grade.SENIOR, 0.8, 0.4);
        assertThat(o.assigned()).isEqualTo(Grade.SENIOR);
        assertThat(o.confirmed()).isFalse();
        assertThat(o.suggestedRetry()).isEqualTo(Grade.JUNIOR);
    }

    @Test
    void firstFailKeepsClaimedTargetUnconfirmed() {
        GradeChangePolicy.Outcome o = policy.resolve(Grade.MIDDLE, null, 0.2, 0.6);
        assertThat(o.assigned()).isEqualTo(Grade.MIDDLE);
        assertThat(o.confirmed()).isFalse();
        assertThat(o.suggestedRetry()).isEqualTo(Grade.JUNIOR);
    }
}
