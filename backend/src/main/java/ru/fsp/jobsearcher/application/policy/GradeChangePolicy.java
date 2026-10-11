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
     * По ТЗ: при провале целевой грейд не подтверждается, но текущий назначенный грейд
     * автоматически не понижается. Кандидату предлагают пересдать на suggestedRetry.
     */
    public Outcome resolve(Grade target, Grade previousAssigned, double overallScore, double juniorFloorScore) {
        if (overallScore >= 0.6 && juniorFloorScore >= 0.5) {
            return new Outcome(target, true, null, "TEST_CONFIRMED");
        }
        Grade keep = previousAssigned != null ? previousAssigned : target;
        Grade suggest = juniorFloorScore < 0.5
                ? Grade.JUNIOR
                : target.lower().orElse(Grade.JUNIOR);
        String reason = juniorFloorScore < 0.5 ? "WEAK_JUNIOR_FLOOR" : "TEST_FAILED";
        return new Outcome(keep, false, suggest, reason);
    }

    public boolean isPassed(double score) {
        return score >= 0.6;
    }

    public record Outcome(
            Grade assigned,
            boolean confirmed,
            Grade suggestedRetry,
            String reasonCode
    ) {
    }
}
