package ru.fsp.jobsearcher.api.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.fsp.jobsearcher.application.security.CurrentUser;
import ru.fsp.jobsearcher.application.security.SecurityUtils;
import ru.fsp.jobsearcher.application.service.ConsentService;
import ru.fsp.jobsearcher.domain.entity.UserConsent;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
@Tag(name = "Auth / Me")
public class MeController {

    private final SecurityUtils securityUtils;
    private final ConsentService consentService;

    @GetMapping
    @Operation(summary = "Текущий пользователь и согласия")
    public Map<String, Object> me() {
        CurrentUser user = securityUtils.requireCurrentUser();
        List<UserConsent> consents = consentService.list(user.userId());
        return Map.of(
                "userId", user.userId(),
                "email", user.email(),
                "role", user.role().name(),
                "emailVerified", user.emailVerified(),
                "consents", consents
        );
    }
}
