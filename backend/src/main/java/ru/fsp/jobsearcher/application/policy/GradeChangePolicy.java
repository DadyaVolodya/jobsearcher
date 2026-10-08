package ru.fsp.jobsearcher.application.policy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import ru.fsp.jobsearcher.api.common.ApiException;
import ru.fsp.jobsearcher.domain.enums.Grade;

public class GradeChangePolicy {

    private final int cooldownDays;
    private final Clock clock;

    public GradeChangePolicy(int cooldownDays, Clock clock) {
        this.cooldownDays = cooldownDays;
        this.clock = clock;
    }

    public int cooldownDays() {
        return cooldownDays;
    }

    public void assertCanChange(Instant lastChangeAt) {
        if (lastChangeAt == null) {
            return;
        }
        Instant threshold = clock.instant().minus(Duration.ofDays(cooldownDays));
        if (lastChangeAt.isAfter(threshold)) {
            throw ApiException.gradeCooldown(
                    "Пересдача / смена грейда доступна не чаще одного раза в " + cooldownDays + " дней");
        }
    }

    /**
     * Если на целевом грейде (например SENIOR) кандидат сдал менее 50% «джун-пола» - автоматом JUNIOR.
     * Иначе при pass (>=0.6) - целевой грейд; при fail - на уровень ниже, но не ниже JUNIOR если пол пройден.
     */
    public Grade resolveAssigned(Grade target, double overallScore, double juniorFloorScore) {
        if (juniorFloorScore < 0.5) {
            return Grade.JUNIOR;
        }
        if (overallScore >= 0.6) {
            return target;
        }
        return target.lower().orElse(Grade.JUNIOR);
    }

    public boolean isConfirmed(Grade target, Grade assigned, double overallScore) {
        return assigned == target && overallScore >= 0.6;
    }

    public boolean isPassed(double score) {
        return score >= 0.6;
    }
}
