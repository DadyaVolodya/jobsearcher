package ru.fsp.jobsearcher.api.invitation;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.fsp.jobsearcher.application.security.SecurityUtils;
import ru.fsp.jobsearcher.application.service.InvitationService;
import ru.fsp.jobsearcher.domain.enums.InvitationStatus;

@RestController
@RequestMapping("/api/v1/invitations")
@RequiredArgsConstructor
@Tag(name = "Invitations")
public class InvitationController {

    private final SecurityUtils securityUtils;
    private final InvitationService invitationService;

    @PostMapping
    public InvitationService.InvitationView create(@RequestBody InvitationService.CreateRequest req) {
        return invitationService.create(securityUtils.requireCurrentUser(), req);
    }

    @GetMapping
    public List<InvitationService.InvitationView> list() {
        return invitationService.listForCurrent(securityUtils.requireCurrentUser());
    }

    @PostMapping("/{id}/status")
    public InvitationService.InvitationView status(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        return invitationService.updateStatus(
                securityUtils.requireCurrentUser(),
                id,
                InvitationStatus.valueOf(body.get("status")),
                body.get("declineReason")
        );
    }
}
