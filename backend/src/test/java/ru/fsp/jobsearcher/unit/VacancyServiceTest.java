package ru.fsp.jobsearcher.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import ru.fsp.jobsearcher.api.common.ApiException;
import ru.fsp.jobsearcher.application.security.CurrentUser;
import ru.fsp.jobsearcher.application.service.CandidateService;
import ru.fsp.jobsearcher.application.service.EmployerService;
import ru.fsp.jobsearcher.application.service.VacancyService;
import ru.fsp.jobsearcher.domain.entity.EmployerProfile;
import ru.fsp.jobsearcher.domain.entity.Vacancy;
import ru.fsp.jobsearcher.domain.enums.UserRole;
import ru.fsp.jobsearcher.domain.enums.VacancyStatus;
import ru.fsp.jobsearcher.domain.repository.VacancyApplicationRepository;
import ru.fsp.jobsearcher.domain.repository.VacancyRepository;

class VacancyServiceTest {
    private final CurrentUser user = new CurrentUser(UUID.randomUUID(), "employer", "e@example.com", UserRole.EMPLOYER, true);
    private final EmployerProfile employer = EmployerProfile.create(user.userId(), "Company");
    private final VacancyRepository vacancies = mock(VacancyRepository.class);
    private final EmployerService employers = mock(EmployerService.class);
    private final VacancyService service = new VacancyService(vacancies, mock(VacancyApplicationRepository.class),
            employers, mock(CandidateService.class));
    private final VacancyService.CreateRequest request = new VacancyService.CreateRequest(
            "Updated", "Description", "IT", "BACKEND", "JUNIOR", List.of("Java"), 100000, 150000, true);

    @Test
    void ownerCanUpdateAndPublishWithoutChangingVacancyIdentity() {
        Vacancy vacancy = existing(employer.getId());
        when(vacancies.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        Vacancy updated = service.update(user, vacancy.getId(), request);
        assertThat(updated.getId()).isEqualTo(vacancy.getId());
        assertThat(updated.getEmployerId()).isEqualTo(employer.getId());
        assertThat(updated.getTitle()).isEqualTo("Updated");
        assertThat(updated.getSalaryTo()).isEqualTo(150000);
        assertThat(updated.getStatus()).isEqualTo(VacancyStatus.PUBLISHED);
        assertThat(updated.getUpdatedAt()).isNotNull();
    }

    @Test
    void cannotEditAnotherEmployersVacancy() {
        Vacancy vacancy = existing(UUID.randomUUID());
        assertThatThrownBy(() -> service.update(user, vacancy.getId(), request))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
        verify(vacancies, never()).save(any());
    }

    private Vacancy existing(UUID owner) {
        Vacancy vacancy = new Vacancy();
        vacancy.setId(UUID.randomUUID());
        vacancy.setEmployerId(owner);
        when(employers.requireMine(user)).thenReturn(employer);
        when(vacancies.findById(vacancy.getId())).thenReturn(Optional.of(vacancy));
        return vacancy;
    }
}
