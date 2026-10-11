package ru.fsp.jobsearcher.application.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.fsp.jobsearcher.api.common.ApiException;
import ru.fsp.jobsearcher.domain.entity.UserConsent;
import ru.fsp.jobsearcher.domain.enums.ConsentType;
import ru.fsp.jobsearcher.domain.repository.UserConsentRepository;

@Service
@RequiredArgsConstructor
public class ConsentService {

    private final UserConsentRepository userConsentRepository;

    @Transactional(readOnly = true)
    public List<ConsentView> list(UUID userId) {
        return userConsentRepository.findByUserId(userId).stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public List<ConsentDocumentCatalog.DocumentMeta> documents() {
        return ConsentDocumentCatalog.all();
    }

    @Transactional(readOnly = true)
    public byte[] documentBody(ConsentType type) {
        ConsentDocumentCatalog.DocumentMeta meta = ConsentDocumentCatalog.require(type);
        try (InputStream in = new ClassPathResource(meta.contentPath()).getInputStream()) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw ApiException.notFound("Текст согласия не найден");
        }
    }

    @Transactional(readOnly = true)
    public String documentTitle(ConsentType type) {
        return ConsentDocumentCatalog.require(type).title();
    }

    @Transactional
    public ConsentView grant(UUID userId, ConsentType type, boolean granted, String documentVersion) {
        ConsentDocumentCatalog.DocumentMeta meta = ConsentDocumentCatalog.require(type);
        if (granted) {
            if (documentVersion == null || documentVersion.isBlank()) {
                throw ApiException.badState(
                        "Для согласия требуется принять вложение документа (documentVersion=" + meta.version() + ")");
            }
            if (!meta.version().equals(documentVersion.trim())) {
                throw ApiException.badState(
                        "Устаревшая версия текста согласия. Актуальная: " + meta.version());
            }
        }
        UserConsent consent = userConsentRepository.findByUserIdAndConsentType(userId, type)
                .orElseGet(() -> {
                    UserConsent c = new UserConsent();
                    c.setId(UUID.randomUUID());
                    c.setUserId(userId);
                    c.setConsentType(type);
                    c.setVersionLabel(meta.version());
                    return c;
                });
        consent.setGranted(granted);
        consent.setVersionLabel(meta.version());
        if (granted) {
            consent.setGrantedAt(Instant.now());
            consent.setRevokedAt(null);
        } else {
            consent.setRevokedAt(Instant.now());
        }
        return toView(userConsentRepository.save(consent));
    }

    @Transactional
    public void grantAllRequired(UUID userId) {
        Arrays.stream(ConsentType.values())
                .forEach(t -> grant(userId, t, true, ConsentDocumentCatalog.CURRENT_VERSION));
    }

    private ConsentView toView(UserConsent c) {
        ConsentDocumentCatalog.DocumentMeta meta = ConsentDocumentCatalog.require(c.getConsentType());
        return new ConsentView(
                c.getId(),
                c.getConsentType(),
                c.isGranted(),
                c.getGrantedAt(),
                c.getRevokedAt(),
                c.getVersionLabel(),
                meta.title(),
                meta.urlPath(),
                meta.version()
        );
    }

    public record ConsentView(
            UUID id,
            ConsentType consentType,
            boolean granted,
            Instant grantedAt,
            Instant revokedAt,
            String versionLabel,
            String documentTitle,
            String documentUrl,
            String currentDocumentVersion
    ) {
    }
}
