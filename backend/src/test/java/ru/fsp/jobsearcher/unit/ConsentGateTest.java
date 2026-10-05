package ru.fsp.jobsearcher.unit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import ru.fsp.jobsearcher.api.common.ApiException;
import ru.fsp.jobsearcher.consents.ConsentGate;
import ru.fsp.jobsearcher.domain.entity.UserConsent;
import ru.fsp.jobsearcher.domain.enums.ConsentType;

class ConsentGateTest {

    @Test
    void requiresBothConsents() {
        UserConsent onlyProcessing = consent(ConsentType.PERSONAL_DATA_PROCESSING, true);
        assertThatThrownBy(() -> ConsentGate.requireForPublication(List.of(onlyProcessing)))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void allowsWhenBothGranted() {
        ConsentGate.requireForPublication(List.of(
                consent(ConsentType.PERSONAL_DATA_PROCESSING, true),
                consent(ConsentType.PROFILE_PUBLICATION, true)
        ));
    }

    private static UserConsent consent(ConsentType type, boolean granted) {
        UserConsent c = new UserConsent();
        c.setId(UUID.randomUUID());
        c.setUserId(UUID.randomUUID());
        c.setConsentType(type);
        c.setGranted(granted);
        c.setVersionLabel("v1");
        return c;
    }
}
