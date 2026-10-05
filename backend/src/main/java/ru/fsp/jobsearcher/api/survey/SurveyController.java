package ru.fsp.jobsearcher.api.survey;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.fsp.jobsearcher.application.security.SecurityUtils;
import ru.fsp.jobsearcher.application.service.SurveyService;
import ru.fsp.jobsearcher.domain.entity.Questionnaire;
import ru.fsp.jobsearcher.domain.entity.SurveyQuestion;
import ru.fsp.jobsearcher.domain.entity.SurveySession;

@RestController
@RequestMapping("/api/v1/surveys")
@RequiredArgsConstructor
@Tag(name = "Surveys")
public class SurveyController {

    private final SecurityUtils securityUtils;
    private final SurveyService surveyService;

    @GetMapping
    public List<Questionnaire> list(
            @RequestParam String audience,
            @RequestParam(required = false) String industryCode
    ) {
        return surveyService.list(audience, industryCode);
    }

    @GetMapping("/{id}/questions")
    public List<SurveyQuestion> questions(@PathVariable UUID id) {
        return surveyService.questions(id);
    }

    @PostMapping("/sessions")
    public SurveySession start(@RequestBody Map<String, String> body) {
        return surveyService.start(securityUtils.requireCurrentUser(), body.get("questionnaireCode"));
    }

    @PostMapping("/sessions/{id}/answers")
    public SurveySession answer(@PathVariable UUID id, @RequestBody Map<String, Object> answers) {
        return surveyService.answer(securityUtils.requireCurrentUser(), id, answers);
    }

    @PostMapping("/sessions/{id}/complete")
    public SurveySession complete(
            @PathVariable UUID id,
            @RequestParam(required = false) UUID employerNeedId
    ) {
        return surveyService.complete(securityUtils.requireCurrentUser(), id, employerNeedId);
    }
}
