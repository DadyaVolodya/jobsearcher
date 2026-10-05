package ru.fsp.jobsearcher.application.policy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import ru.fsp.jobsearcher.api.common.ApiException;
import ru.fsp.jobsearcher.domain.enums.Grade;

public class GradeChangePolicy {

    private final int cooldownDays;
    private final Clock clock;

    public GradeChangePolicy(int cooldownDays, Clock clock) {
        this.cooldownDays = cooldownDays;
        this.clock = clock;
    }

    public void assertCanChange(Instant lastChangeAt) {
        if (lastChangeAt == null) {
            return;
        }
        Instant threshold = clock.instant().minus(Duration.ofDays(cooldownDays));
        if (lastChangeAt.isAfter(threshold)) {
            throw ApiException.gradeCooldown(
                    "Смена грейда доступна не чаще одного раза в " + cooldownDays + " дней");
        }
    }

    public Grade resolveAfterTest(Grade target, boolean passed, double score) {
        if (passed) {
            if (score >= 0.9) {
                return target.higher().orElse(target);
            }
            return target;
        }
        Optional<Grade> lower = target.lower();
        return lower.orElse(target);
    }

    public boolean isPassed(double score) {
        return score >= 0.6;
    }
}
