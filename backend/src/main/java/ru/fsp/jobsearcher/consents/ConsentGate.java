package ru.fsp.jobsearcher.consents;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import ru.fsp.jobsearcher.api.common.ApiException;
import ru.fsp.jobsearcher.domain.entity.UserConsent;
import ru.fsp.jobsearcher.domain.enums.ConsentType;

public final class ConsentGate {

    private ConsentGate() {
    }

    public static void requireForPublication(List<UserConsent> consents) {
        Set<ConsentType> granted = EnumSet.noneOf(ConsentType.class);
        for (UserConsent c : consents) {
            if (c.isGranted()) {
                granted.add(c.getConsentType());
            }
        }
        if (!granted.contains(ConsentType.PERSONAL_DATA_PROCESSING)) {
            throw ApiException.consentRequired("Требуется согласие на обработку персональных данных (152-ФЗ)");
        }
        if (!granted.contains(ConsentType.PROFILE_PUBLICATION)) {
            throw ApiException.consentRequired("Требуется согласие на публикацию профиля");
        }
    }

    public static boolean has(List<UserConsent> consents, ConsentType type) {
        return consents.stream().anyMatch(c -> c.getConsentType() == type && c.isGranted());
    }
}
