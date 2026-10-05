package ru.fsp.jobsearcher.api.vacancy;

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
import ru.fsp.jobsearcher.application.service.VacancyService;
import ru.fsp.jobsearcher.domain.entity.Vacancy;
import ru.fsp.jobsearcher.domain.entity.VacancyApplication;

@RestController
@RequestMapping("/api/v1/vacancies")
@RequiredArgsConstructor
@Tag(name = "Vacancies")
public class VacancyController {

    private final SecurityUtils securityUtils;
    private final VacancyService vacancyService;

    @GetMapping
    public List<Vacancy> published() {
        return vacancyService.listPublished();
    }

    @GetMapping("/mine")
    public List<Vacancy> mine() {
        return vacancyService.listMine(securityUtils.requireCurrentUser());
    }

    @PostMapping
    public Vacancy create(@RequestBody VacancyService.CreateRequest req) {
        return vacancyService.create(securityUtils.requireCurrentUser(), req);
    }

    @PostMapping("/{id}/applications")
    public VacancyApplication apply(@PathVariable UUID id, @RequestBody(required = false) Map<String, String> body) {
        String letter = body == null ? null : body.get("coverLetter");
        return vacancyService.apply(securityUtils.requireCurrentUser(), id, letter);
    }

    @GetMapping("/applications/mine")
    public List<VacancyApplication> myApplications() {
        return vacancyService.myApplications(securityUtils.requireCurrentUser());
    }

    @GetMapping("/{id}/applications")
    public List<VacancyApplication> vacancyApplications(@PathVariable UUID id) {
        return vacancyService.vacancyApplications(securityUtils.requireCurrentUser(), id);
    }
}
