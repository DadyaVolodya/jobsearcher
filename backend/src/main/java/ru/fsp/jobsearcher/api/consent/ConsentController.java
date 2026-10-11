package ru.fsp.jobsearcher.api.consent;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.fsp.jobsearcher.application.security.SecurityUtils;
import ru.fsp.jobsearcher.application.service.ConsentDocumentCatalog;
import ru.fsp.jobsearcher.application.service.ConsentService;
import ru.fsp.jobsearcher.domain.enums.ConsentType;

@RestController
@RequestMapping("/api/v1/consents")
@RequiredArgsConstructor
@Tag(name = "Consents")
public class ConsentController {

    private final SecurityUtils securityUtils;
    private final ConsentService consentService;

    @GetMapping
    public List<ConsentService.ConsentView> list() {
        return consentService.list(securityUtils.requireUserId());
    }

    /** Метаданные вложений к согласиям (текст 152-ФЗ и публикация профиля). */
    @GetMapping("/documents")
    public List<ConsentDocumentCatalog.DocumentMeta> documents() {
        return consentService.documents();
    }

    /** Полный текст согласия как вложение (HTML). */
    @GetMapping("/documents/{type}")
    public ResponseEntity<byte[]> document(@PathVariable ConsentType type) {
        byte[] body = consentService.documentBody(type);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "html", StandardCharsets.UTF_8));
        headers.setContentDisposition(ContentDisposition.inline()
                .filename(type.name().toLowerCase() + "-" + ConsentDocumentCatalog.CURRENT_VERSION + ".html",
                        StandardCharsets.UTF_8)
                .build());
        return ResponseEntity.ok().headers(headers).body(body);
    }

    @PostMapping
    public ConsentService.ConsentView grant(@Valid @RequestBody ConsentRequest req) {
        return consentService.grant(
                securityUtils.requireUserId(),
                req.type(),
                req.granted(),
                req.documentVersion()
        );
    }

    public record ConsentRequest(
            @NotNull ConsentType type,
            boolean granted,
            String documentVersion
    ) {
    }
}
