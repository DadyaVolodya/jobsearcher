package ru.fsp.jobsearcher.application.service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import ru.fsp.jobsearcher.domain.enums.ConsentType;

/** Каталог текстов согласий (вложения к чекбоксам 152-ФЗ). */
public final class ConsentDocumentCatalog {

    public static final String CURRENT_VERSION = "v1";

    public record DocumentMeta(
            ConsentType type,
            String version,
            String title,
            String contentPath,
            String urlPath
    ) {
    }

    private static final Map<ConsentType, DocumentMeta> DOCS = new EnumMap<>(ConsentType.class);

    static {
        DOCS.put(ConsentType.PERSONAL_DATA_PROCESSING, new DocumentMeta(
                ConsentType.PERSONAL_DATA_PROCESSING,
                CURRENT_VERSION,
                "Согласие на обработку персональных данных (152-ФЗ)",
                "legal/personal-data-consent-v1.html",
                "/api/v1/consents/documents/PERSONAL_DATA_PROCESSING"
        ));
        DOCS.put(ConsentType.PROFILE_PUBLICATION, new DocumentMeta(
                ConsentType.PROFILE_PUBLICATION,
                CURRENT_VERSION,
                "Согласие на публикацию профиля",
                "legal/profile-publication-v1.html",
                "/api/v1/consents/documents/PROFILE_PUBLICATION"
        ));
    }

    private ConsentDocumentCatalog() {
    }

    public static DocumentMeta require(ConsentType type) {
        DocumentMeta meta = DOCS.get(type);
        if (meta == null) {
            throw new IllegalArgumentException("Нет документа для " + type);
        }
        return meta;
    }

    public static List<DocumentMeta> all() {
        return List.copyOf(DOCS.values());
    }
}
