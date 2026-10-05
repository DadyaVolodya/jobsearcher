package ru.fsp.jobsearcher.api.employer;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.fsp.jobsearcher.application.security.SecurityUtils;
import ru.fsp.jobsearcher.application.service.EmployerService;
import ru.fsp.jobsearcher.domain.entity.EmployerNeed;
import ru.fsp.jobsearcher.domain.entity.EmployerProfile;

@RestController
@RequestMapping("/api/v1/employer")
@RequiredArgsConstructor
@Tag(name = "Employer LK")
public class EmployerController {

    private final SecurityUtils securityUtils;
    private final EmployerService employerService;

    @GetMapping("/profile")
    public EmployerProfile profile() {
        return employerService.requireMine(securityUtils.requireCurrentUser());
    }

    @PutMapping("/profile")
    public EmployerProfile update(@RequestBody EmployerService.UpdateRequest req) {
        return employerService.update(securityUtils.requireCurrentUser(), req);
    }

    @GetMapping("/needs")
    public List<EmployerNeed> needs() {
        return employerService.listNeeds(securityUtils.requireCurrentUser());
    }

    @PostMapping("/needs")
    public EmployerNeed createNeed(@RequestBody EmployerService.NeedRequest req) {
        return employerService.createNeed(securityUtils.requireCurrentUser(), req);
    }
}
