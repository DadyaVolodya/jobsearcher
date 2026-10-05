package ru.fsp.jobsearcher.api.consent;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.fsp.jobsearcher.application.security.SecurityUtils;
import ru.fsp.jobsearcher.application.service.ConsentService;
import ru.fsp.jobsearcher.domain.entity.UserConsent;
import ru.fsp.jobsearcher.domain.enums.ConsentType;

@RestController
@RequestMapping("/api/v1/consents")
@RequiredArgsConstructor
@Tag(name = "Consents")
public class ConsentController {

    private final SecurityUtils securityUtils;
    private final ConsentService consentService;

    @GetMapping
    public List<UserConsent> list() {
        return consentService.list(securityUtils.requireUserId());
    }

    @PostMapping
    public UserConsent grant(@Valid @RequestBody ConsentRequest req) {
        return consentService.grant(securityUtils.requireUserId(), req.type(), req.granted());
    }

    public record ConsentRequest(@NotNull ConsentType type, boolean granted) {}
}
