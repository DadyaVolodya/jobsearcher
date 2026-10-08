package ru.fsp.jobsearcher.application.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.fsp.jobsearcher.api.common.ApiException;
import ru.fsp.jobsearcher.application.policy.CategoryCodes;
import ru.fsp.jobsearcher.application.policy.GradeChangePolicy;
import ru.fsp.jobsearcher.application.security.CurrentUser;
import ru.fsp.jobsearcher.domain.entity.CandidateProfile;
import ru.fsp.jobsearcher.domain.entity.GradeChangeLog;
import ru.fsp.jobsearcher.domain.entity.TestItem;
import ru.fsp.jobsearcher.domain.entity.TestSession;
import ru.fsp.jobsearcher.domain.enums.Grade;
import ru.fsp.jobsearcher.domain.enums.SessionStatus;
import ru.fsp.jobsearcher.domain.enums.UserRole;
import ru.fsp.jobsearcher.domain.repository.CandidateProfileRepository;
import ru.fsp.jobsearcher.domain.repository.GradeChangeLogRepository;
import ru.fsp.jobsearcher.domain.repository.TestItemRepository;
import ru.fsp.jobsearcher.domain.repository.TestSessionRepository;
import ru.fsp.jobsearcher.testing.engine.TestSessionAssembler;

@Service
@RequiredArgsConstructor
public class TestingService {

    private final CandidateService candidateService;
    private final CandidateProfileRepository candidateProfileRepository;
    private final TestItemRepository testItemRepository;
    private final TestSessionRepository testSessionRepository;
    private final GradeChangeLogRepository gradeChangeLogRepository;
    private final GradeChangePolicy gradeChangePolicy;

    @Transactional
    public TestSession start(CurrentUser user, String targetGrade) {
        CandidateProfile profile = candidateService.requireMine(user);
        if (profile.getIndustryCode() == null || profile.getSpecializationCode() == null) {
            throw ApiException.badState("Сначала завершите отраслевую анкету (отрасль и специализация)");
        }
        Grade grade = Grade.from(targetGrade == null
                ? (profile.getClaimedGrade() == null ? "JUNIOR" : profile.getClaimedGrade().name())
                : targetGrade);
        gradeChangePolicy.assertCanChange(profile.getLastGradeChangeAt());
        profile.setClaimedGrade(grade);
        candidateProfileRepository.save(profile);

        UUID attemptId = UUID.randomUUID();
        long seed = TestSessionAssembler.seedOf(profile.getId(), attemptId);
        List<TestItem> bank = loadBank(profile.getIndustryCode(), profile.getSpecializationCode(), grade);
        if (bank.isEmpty()) {
            throw ApiException.badState("Нет заданий для выбранной категории");
        }
        List<Map<String, Object>> items = TestSessionAssembler.assemble(bank, seed, Math.min(8, bank.size()));
        // strip expected for client response later in controller DTO if needed - keep server-side for grading
        List<Map<String, Object>> publicItems = items.stream().map(this::publicItem).toList();

        TestSession session = new TestSession();
        session.setId(attemptId);
        session.setCandidateId(profile.getId());
        session.setTargetGrade(grade);
        session.setIndustryCode(profile.getIndustryCode());
        session.setSpecializationCode(profile.getSpecializationCode());
        session.setSeed(seed);
        session.setStatus(SessionStatus.IN_PROGRESS);
        session.setItems(items);
        session.setAnswers(new HashMap<>());
        session.setProctorEvents(new ArrayList<>());
        session.setSectionScores(new HashMap<>());
        session.setStartedAt(Instant.now());
        TestSession saved = testSessionRepository.save(session);
        saved.setItems(publicItems);
        return saved;
    }

    @Transactional
    public TestSession submit(CurrentUser user, UUID sessionId, Map<String, Object> answers) {
        CandidateProfile profile = candidateService.requireMine(user);
        TestSession session = ownedInProgress(user, profile, sessionId);
        Map<String, Object> merged = new HashMap<>(session.getAnswers());
        merged.putAll(answers);
        session.setAnswers(merged);
        return finalizeSession(profile, session, null);
    }

    @Transactional
    public TestSession proctorEvent(CurrentUser user, UUID sessionId, String event, String detail) {
        CandidateProfile profile = candidateService.requireMine(user);
        TestSession session = ownedInProgress(user, profile, sessionId);
        List<Map<String, Object>> events = new ArrayList<>(session.getProctorEvents());
        Map<String, Object> ev = new LinkedHashMap<>();
        ev.put("event", event);
        ev.put("detail", detail);
        ev.put("at", Instant.now().toString());
        events.add(ev);
        session.setProctorEvents(events);

        String normalized = event == null ? "" : event.toUpperCase();
        if (normalized.contains("BLUR") || normalized.contains("TAB") || normalized.contains("HIDDEN")
                || normalized.contains("FOCUS_LOST") || normalized.contains("LEAVE")) {
            session.setAnswers(session.getAnswers());
            return finalizeSession(profile, session, "LEFT_WINDOW");
        }
        if (normalized.contains("COPY")) {
            // soft signal - keep going but record
            return testSessionRepository.save(session);
        }
        return testSessionRepository.save(session);
    }

    @Transactional(readOnly = true)
    public TestSession get(CurrentUser user, UUID sessionId) {
        CandidateProfile profile = candidateService.requireMine(user);
        TestSession session = testSessionRepository.findById(sessionId)
                .orElseThrow(() -> ApiException.notFound("Тест не найден"));
        if (!session.getCandidateId().equals(profile.getId()) && user.role() != UserRole.ADMIN) {
            throw ApiException.forbidden("Чужой тест");
        }
        session.setItems(session.getItems().stream().map(this::publicItem).toList());
        return session;
    }

    private TestSession finalizeSession(CandidateProfile profile, TestSession session, String failReason) {
        List<String> ids = session.getItems().stream().map(i -> String.valueOf(i.get("itemId"))).toList();
        Map<String, TestItem> byId = testItemRepository.findAllById(
                        ids.stream().map(UUID::fromString).toList()).stream()
                .collect(Collectors.toMap(i -> i.getId().toString(), Function.identity()));

        TestSessionAssembler.ScoreBreakdown breakdown = TestSessionAssembler.gradeAnswers(
                session.getItems(), session.getAnswers(), byId);

        Grade assigned;
        boolean confirmed;
        boolean passed;
        if (failReason != null) {
            assigned = Grade.JUNIOR;
            confirmed = false;
            passed = false;
            session.setFailReason(failReason);
            session.setScore(0.0);
            session.setJuniorFloorScore(0.0);
            session.setSectionScores(Map.of("A", 0.0, "B", 0.0, "C", 0.0));
        } else {
            assigned = gradeChangePolicy.resolveAssigned(
                    session.getTargetGrade(), breakdown.overall(), breakdown.juniorFloor());
            confirmed = gradeChangePolicy.isConfirmed(session.getTargetGrade(), assigned, breakdown.overall());
            passed = confirmed;
            session.setScore(breakdown.overall());
            session.setJuniorFloorScore(breakdown.juniorFloor());
            session.setSectionScores(breakdown.bySection());
        }

        session.setPassed(passed);
        session.setGradeConfirmed(confirmed);
        session.setAssignedGrade(assigned);
        session.setStatus(SessionStatus.COMPLETED);
        session.setCompletedAt(Instant.now());
        testSessionRepository.save(session);

        Grade previous = profile.getAssignedGrade();
        profile.setAssignedGrade(assigned);
        profile.setGradeConfirmed(confirmed);
        profile.setTestScore(session.getScore() == null ? 0.0 : session.getScore());
        profile.setCategoryCode(CategoryCodes.of(
                session.getIndustryCode(), session.getSpecializationCode(), assigned));
        profile.setProfileScore(0.7 * profile.getTestScore() + 0.3 * averageVector(profile.getSurveyVector()));
        profile.setLastGradeChangeAt(Instant.now());
        profile.touch();
        candidateProfileRepository.save(profile);

        GradeChangeLog log = new GradeChangeLog();
        log.setId(UUID.randomUUID());
        log.setCandidateId(profile.getId());
        log.setFromGrade(previous == null ? null : previous.name());
        log.setToGrade(assigned.name());
        log.setReason(failReason != null ? failReason : (confirmed ? "TEST_CONFIRMED" : "TEST_UNCONFIRMED_OR_FLOOR"));
        log.setChangedAt(Instant.now());
        gradeChangeLogRepository.save(log);

        session.setItems(session.getItems().stream().map(this::publicItem).toList());
        return session;
    }

    private TestSession ownedInProgress(CurrentUser user, CandidateProfile profile, UUID sessionId) {
        TestSession session = testSessionRepository.findById(sessionId)
                .orElseThrow(() -> ApiException.notFound("Тест не найден"));
        if (!session.getCandidateId().equals(profile.getId()) && user.role() != UserRole.ADMIN) {
            throw ApiException.forbidden("Чужой тест");
        }
        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw ApiException.badState("Тест уже завершён"
                    + (session.getFailReason() != null
                    ? ". Причина: " + humanFail(session.getFailReason()) : ""));
        }
        return session;
    }

    private List<TestItem> loadBank(String industry, String spec, Grade target) {
        List<TestItem> targetItems = testItemRepository.findForSession(industry, spec, target);
        if (target == Grade.JUNIOR || target == Grade.INTERN) {
            return targetItems;
        }
        List<TestItem> junior = testItemRepository.findForSession(industry, spec, Grade.JUNIOR);
        return Stream.concat(junior.stream(), targetItems.stream())
                .collect(Collectors.toMap(TestItem::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new))
                .values().stream().toList();
    }

    private Map<String, Object> publicItem(Map<String, Object> item) {
        Map<String, Object> copy = new LinkedHashMap<>(item);
        copy.remove("expected");
        return copy;
    }

    private static String humanFail(String code) {
        if ("LEFT_WINDOW".equals(code)) {
            return "вы ушли со вкладки/окна теста - попытка засчитана как провал";
        }
        return code;
    }

    private static double averageVector(Map<String, Double> vector) {
        if (vector == null || vector.isEmpty()) {
            return 0.0;
        }
        return vector.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }
}
