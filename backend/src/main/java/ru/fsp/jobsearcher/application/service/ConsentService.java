package ru.fsp.jobsearcher.application.service;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.fsp.jobsearcher.domain.entity.UserConsent;
import ru.fsp.jobsearcher.domain.enums.ConsentType;
import ru.fsp.jobsearcher.domain.repository.UserConsentRepository;

@Service
@RequiredArgsConstructor
public class ConsentService {

    private final UserConsentRepository userConsentRepository;

    @Transactional(readOnly = true)
    public List<UserConsent> list(UUID userId) {
        return userConsentRepository.findByUserId(userId);
    }

    @Transactional
    public UserConsent grant(UUID userId, ConsentType type, boolean granted) {
        UserConsent consent = userConsentRepository.findByUserIdAndConsentType(userId, type)
                .orElseGet(() -> {
                    UserConsent c = new UserConsent();
                    c.setId(UUID.randomUUID());
                    c.setUserId(userId);
                    c.setConsentType(type);
                    c.setVersionLabel("v1");
                    return c;
                });
        consent.setGranted(granted);
        if (granted) {
            consent.setGrantedAt(Instant.now());
            consent.setRevokedAt(null);
        } else {
            consent.setRevokedAt(Instant.now());
        }
        return userConsentRepository.save(consent);
    }

    @Transactional
    public void grantAllRequired(UUID userId) {
        Arrays.stream(ConsentType.values()).forEach(t -> grant(userId, t, true));
    }
}
