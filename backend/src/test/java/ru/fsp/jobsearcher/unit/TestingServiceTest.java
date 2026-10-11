package ru.fsp.jobsearcher.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import ru.fsp.jobsearcher.application.policy.GradeChangePolicy;
import ru.fsp.jobsearcher.application.security.CurrentUser;
import ru.fsp.jobsearcher.application.service.CandidateService;
import ru.fsp.jobsearcher.application.service.TestingService;
import ru.fsp.jobsearcher.domain.entity.CandidateProfile;
import ru.fsp.jobsearcher.domain.entity.TestItem;
import ru.fsp.jobsearcher.domain.entity.TestSession;
import ru.fsp.jobsearcher.domain.enums.Grade;
import ru.fsp.jobsearcher.domain.enums.QuestionType;
import ru.fsp.jobsearcher.domain.enums.UserRole;
import ru.fsp.jobsearcher.domain.repository.CandidateProfileRepository;
import ru.fsp.jobsearcher.domain.repository.GradeChangeLogRepository;
import ru.fsp.jobsearcher.domain.repository.TestItemRepository;
import ru.fsp.jobsearcher.domain.repository.TestSessionRepository;

class TestingServiceTest {
    @Test
    void publicResponsesHideAnswersWithoutChangingStoredSessionOrGrading() {
        CurrentUser user = new CurrentUser(UUID.randomUUID(), "test", "test@example.com", UserRole.CANDIDATE, true);
        CandidateProfile profile = CandidateProfile.create(user.userId());
        profile.setIndustryCode("IT");
        profile.setSpecializationCode("BACKEND");
        TestItem item = new TestItem();
        item.setId(UUID.randomUUID());
        item.setCode("SUM");
        item.setGrade(Grade.JUNIOR);
        item.setSection("B");
        item.setQuestionType(QuestionType.SINGLE_CHOICE);
        item.setPromptTemplate("{{a}} + {{b}}");
        item.setWeight(1);
        item.setCorrectAnswer(Map.of("mode", "PARAM_SUM"));
        item.setParamSchema(Map.of("a", Map.of("min", 2, "max", 2), "b", Map.of("min", 3, "max", 3)));

        var candidates = mock(CandidateService.class);
        var profiles = mock(CandidateProfileRepository.class);
        var items = mock(TestItemRepository.class);
        var sessions = mock(TestSessionRepository.class);
        var logs = mock(GradeChangeLogRepository.class);
        when(candidates.requireMine(user)).thenReturn(profile);
        when(items.findForSession("IT", "BACKEND", Grade.JUNIOR)).thenReturn(List.of(item));
        when(items.findAllById(anyList())).thenReturn(List.of(item));
        TestSession[] stored = new TestSession[1];
        when(sessions.save(any())).thenAnswer(invocation -> {
            stored[0] = invocation.getArgument(0);
            return stored[0];
        });
        when(sessions.findById(any())).thenAnswer(invocation -> Optional.ofNullable(stored[0]));
        var service = new TestingService(candidates, profiles, items, sessions, logs,
                new GradeChangePolicy(30, Clock.systemUTC()));

        TestSession started = service.start(user, "JUNIOR");
        assertPublic(started, stored[0]);
        assertPublic(service.get(user, started.getId()), stored[0]);
        assertPublic(service.proctorEvent(user, started.getId(), "COPY_ATTEMPT", "copy"), stored[0]);
        assertThat(stored[0].getProctorEvents()).hasSize(1);
        TestSession submitted = service.submit(user, started.getId(), Map.of("SUM", "5"));
        assertPublic(submitted, stored[0]);
        assertThat(submitted.getScore()).isEqualTo(1.0);
        assertThat(submitted.getGradeConfirmed()).isTrue();
    }

    private void assertPublic(TestSession response, TestSession stored) {
        assertThat(response).isNotSameAs(stored);
        assertThat(response.getItems().getFirst()).doesNotContainKey("expected");
        assertThat(stored.getItems().getFirst()).containsEntry("expected", "5");
    }
}
