package ru.fsp.jobsearcher.api.testing;

import io.swagger.v3.oas.annotations.tags.Tag;
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
import ru.fsp.jobsearcher.application.service.TestingService;
import ru.fsp.jobsearcher.domain.entity.TestSession;

@RestController
@RequestMapping("/api/v1/tests")
@RequiredArgsConstructor
@Tag(name = "Testing")
public class TestingController {

    private final SecurityUtils securityUtils;
    private final TestingService testingService;

    @PostMapping("/sessions")
    public TestSession start(@RequestBody(required = false) Map<String, String> body) {
        String grade = body == null ? null : body.get("targetGrade");
        return testingService.start(securityUtils.requireCurrentUser(), grade);
    }

    @GetMapping("/sessions/{id}")
    public TestSession get(@PathVariable UUID id) {
        return testingService.get(securityUtils.requireCurrentUser(), id);
    }

    @PostMapping("/sessions/{id}/submit")
    public TestSession submit(@PathVariable UUID id, @RequestBody Map<String, Object> answers) {
        return testingService.submit(securityUtils.requireCurrentUser(), id, answers);
    }

    /**
     * События прокторинга с фронта: BLUR / TAB_HIDDEN / FOCUS_LOST / LEAVE_WINDOW / COPY_ATTEMPT.
     * Уход с окна валит попытку (failReason=LEFT_WINDOW).
     */
    @PostMapping("/sessions/{id}/proctor")
    public TestSession proctor(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        return testingService.proctorEvent(
                securityUtils.requireCurrentUser(),
                id,
                body.get("event"),
                body.get("detail")
        );
    }
}
