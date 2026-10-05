package ru.fsp.jobsearcher.application.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.fsp.jobsearcher.api.common.ApiException;
import ru.fsp.jobsearcher.application.security.CurrentUser;
import ru.fsp.jobsearcher.domain.entity.CandidateProfile;
import ru.fsp.jobsearcher.domain.entity.EmployerProfile;
import ru.fsp.jobsearcher.domain.entity.Vacancy;
import ru.fsp.jobsearcher.domain.entity.VacancyApplication;
import ru.fsp.jobsearcher.domain.enums.ApplicationStatus;
import ru.fsp.jobsearcher.domain.enums.Grade;
import ru.fsp.jobsearcher.domain.enums.VacancyStatus;
import ru.fsp.jobsearcher.domain.repository.VacancyApplicationRepository;
import ru.fsp.jobsearcher.domain.repository.VacancyRepository;

@Service
@RequiredArgsConstructor
public class VacancyService {

    private final VacancyRepository vacancyRepository;
    private final VacancyApplicationRepository vacancyApplicationRepository;
    private final EmployerService employerService;
    private final CandidateService candidateService;

    @Transactional
    public Vacancy create(CurrentUser user, CreateRequest req) {
        EmployerProfile employer = employerService.requireMine(user);
        if (req.salaryFrom() <= 0 || req.salaryTo() < req.salaryFrom()) {
            throw ApiException.badState("Зарплата обязательна");
        }
        Vacancy v = new Vacancy();
        v.setId(UUID.randomUUID());
        v.setEmployerId(employer.getId());
        v.setTitle(req.title());
        v.setDescription(req.description());
        v.setIndustryCode(req.industryCode());
        v.setSpecializationCode(req.specializationCode());
        v.setGrade(Grade.from(req.grade()));
        v.setStack(req.stack() == null ? List.of() : req.stack());
        v.setSalaryFrom(req.salaryFrom());
        v.setSalaryTo(req.salaryTo());
        v.setStatus(req.publish() ? VacancyStatus.PUBLISHED : VacancyStatus.DRAFT);
        Instant now = Instant.now();
        v.setCreatedAt(now);
        v.setUpdatedAt(now);
        return vacancyRepository.save(v);
    }

    @Transactional(readOnly = true)
    public List<Vacancy> listPublished() {
        return vacancyRepository.findByStatus(VacancyStatus.PUBLISHED);
    }

    @Transactional(readOnly = true)
    public List<Vacancy> listMine(CurrentUser user) {
        return vacancyRepository.findByEmployerIdOrderByCreatedAtDesc(employerService.requireMine(user).getId());
    }

    @Transactional
    public VacancyApplication apply(CurrentUser user, UUID vacancyId, String coverLetter) {
        CandidateProfile candidate = candidateService.requireMine(user);
        Vacancy vacancy = vacancyRepository.findById(vacancyId)
                .orElseThrow(() -> ApiException.notFound("Вакансия не найдена"));
        if (vacancy.getStatus() != VacancyStatus.PUBLISHED) {
            throw ApiException.badState("Вакансия не опубликована");
        }
        vacancyApplicationRepository.findByVacancyIdAndCandidateId(vacancyId, candidate.getId())
                .ifPresent(a -> {
                    throw ApiException.conflict("Отклик уже существует");
                });
        VacancyApplication app = new VacancyApplication();
        app.setId(UUID.randomUUID());
        app.setVacancyId(vacancyId);
        app.setCandidateId(candidate.getId());
        app.setCoverLetter(coverLetter);
        app.setStatus(ApplicationStatus.SENT);
        Instant now = Instant.now();
        app.setCreatedAt(now);
        app.setUpdatedAt(now);
        return vacancyApplicationRepository.save(app);
    }

    @Transactional(readOnly = true)
    public List<VacancyApplication> myApplications(CurrentUser user) {
        return vacancyApplicationRepository.findByCandidateIdOrderByCreatedAtDesc(
                candidateService.requireMine(user).getId());
    }

    @Transactional(readOnly = true)
    public List<VacancyApplication> vacancyApplications(CurrentUser user, UUID vacancyId) {
        EmployerProfile employer = employerService.requireMine(user);
        Vacancy vacancy = vacancyRepository.findById(vacancyId)
                .orElseThrow(() -> ApiException.notFound("Вакансия не найдена"));
        if (!vacancy.getEmployerId().equals(employer.getId())) {
            throw ApiException.forbidden("Чужая вакансия");
        }
        return vacancyApplicationRepository.findByVacancyIdOrderByCreatedAtDesc(vacancyId);
    }

    public record CreateRequest(
            String title,
            String description,
            String industryCode,
            String specializationCode,
            String grade,
            List<String> stack,
            int salaryFrom,
            int salaryTo,
            boolean publish
    ) {
    }
}
