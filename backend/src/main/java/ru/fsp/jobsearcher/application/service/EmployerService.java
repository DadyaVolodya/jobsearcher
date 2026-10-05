package ru.fsp.jobsearcher.application.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.fsp.jobsearcher.api.common.ApiException;
import ru.fsp.jobsearcher.application.security.CurrentUser;
import ru.fsp.jobsearcher.domain.entity.EmployerNeed;
import ru.fsp.jobsearcher.domain.entity.EmployerProfile;
import ru.fsp.jobsearcher.domain.enums.Grade;
import ru.fsp.jobsearcher.domain.enums.UserRole;
import ru.fsp.jobsearcher.domain.repository.EmployerNeedRepository;
import ru.fsp.jobsearcher.domain.repository.EmployerProfileRepository;

@Service
@RequiredArgsConstructor
public class EmployerService {

    private final EmployerProfileRepository employerProfileRepository;
    private final EmployerNeedRepository employerNeedRepository;

    @Transactional(readOnly = true)
    public EmployerProfile requireMine(CurrentUser user) {
        if (user.role() != UserRole.EMPLOYER && user.role() != UserRole.ADMIN) {
            throw ApiException.forbidden("Только работодатель");
        }
        return employerProfileRepository.findByUserId(user.userId())
                .orElseThrow(() -> ApiException.notFound("Профиль работодателя не найден"));
    }

    @Transactional
    public EmployerProfile update(CurrentUser user, UpdateRequest req) {
        EmployerProfile p = requireMine(user);
        if (req.companyName() != null) {
            p.setCompanyName(req.companyName());
        }
        if (req.description() != null) {
            p.setDescription(req.description());
        }
        if (req.industryFocus() != null) {
            p.setIndustryFocus(req.industryFocus());
        }
        if (req.contactEmail() != null) {
            p.setContactEmail(req.contactEmail());
        }
        if (req.contactPhone() != null) {
            p.setContactPhone(req.contactPhone());
        }
        if (req.website() != null) {
            p.setWebsite(req.website());
        }
        p.touch();
        return employerProfileRepository.save(p);
    }

    @Transactional
    public EmployerNeed createNeed(CurrentUser user, NeedRequest req) {
        EmployerProfile employer = requireMine(user);
        if (req.salaryFrom() <= 0 || req.salaryTo() < req.salaryFrom()) {
            throw ApiException.badState("Зарплата обязательна: от–до в рублях");
        }
        EmployerNeed need = new EmployerNeed();
        need.setId(UUID.randomUUID());
        need.setEmployerId(employer.getId());
        need.setTitle(req.title());
        need.setDescription(req.description());
        need.setIndustryCode(req.industryCode());
        need.setSpecializationCode(req.specializationCode());
        need.setGrade(Grade.from(req.grade()));
        need.setStack(req.stack() == null ? List.of() : req.stack());
        need.setSalaryFrom(req.salaryFrom());
        need.setSalaryTo(req.salaryTo());
        need.setNeedVector(req.needVector() == null ? java.util.Map.of() : req.needVector());
        need.setActive(true);
        Instant now = Instant.now();
        need.setCreatedAt(now);
        need.setUpdatedAt(now);
        return employerNeedRepository.save(need);
    }

    @Transactional(readOnly = true)
    public List<EmployerNeed> listNeeds(CurrentUser user) {
        return employerNeedRepository.findByEmployerIdAndActiveTrue(requireMine(user).getId());
    }

    public record UpdateRequest(
            String companyName,
            String description,
            String industryFocus,
            String contactEmail,
            String contactPhone,
            String website
    ) {
    }

    public record NeedRequest(
            String title,
            String description,
            String industryCode,
            String specializationCode,
            String grade,
            List<String> stack,
            int salaryFrom,
            int salaryTo,
            java.util.Map<String, Double> needVector
    ) {
    }
}
