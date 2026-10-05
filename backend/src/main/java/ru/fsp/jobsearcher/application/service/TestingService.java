package ru.fsp.jobsearcher.application.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
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
        if (profile.getAssignedGrade() != null && profile.getAssignedGrade() != grade) {
            gradeChangePolicy.assertCanChange(profile.getLastGradeChangeAt());
        }
        profile.setClaimedGrade(grade);
        candidateProfileRepository.save(profile);

        UUID attemptId = UUID.randomUUID();
        long seed = TestSessionAssembler.seedOf(profile.getId(), attemptId);
        List<TestItem> bank = testItemRepository.findForSession(
                profile.getIndustryCode(), profile.getSpecializationCode(), grade);
        if (bank.isEmpty()) {
            bank = testItemRepository.findForSession(profile.getIndustryCode(), profile.getSpecializationCode(), Grade.INTERN);
        }
        if (bank.isEmpty()) {
            throw ApiException.badState("Нет заданий для выбранной категории");
        }
        List<Map<String, Object>> items = TestSessionAssembler.assemble(bank, seed, Math.min(5, bank.size()));

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
        session.setStartedAt(Instant.now());
        return testSessionRepository.save(session);
    }

    @Transactional
    public TestSession submit(CurrentUser user, UUID sessionId, Map<String, Object> answers) {
        CandidateProfile profile = candidateService.requireMine(user);
        TestSession session = testSessionRepository.findById(sessionId)
                .orElseThrow(() -> ApiException.notFound("Тест не найден"));
        if (!session.getCandidateId().equals(profile.getId()) && user.role() != UserRole.ADMIN) {
            throw ApiException.forbidden("Чужой тест");
        }
        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw ApiException.badState("Тест уже завершён");
        }
        Map<String, Object> merged = new HashMap<>(session.getAnswers());
        merged.putAll(answers);
        session.setAnswers(merged);

        List<String> ids = session.getItems().stream().map(i -> String.valueOf(i.get("itemId"))).toList();
        Map<String, TestItem> byId = testItemRepository.findAllById(
                        ids.stream().map(UUID::fromString).toList()).stream()
                .collect(Collectors.toMap(i -> i.getId().toString(), Function.identity()));
        double score = TestSessionAssembler.gradeAnswers(session.getItems(), merged, byId);
        boolean passed = gradeChangePolicy.isPassed(score);
        Grade assigned = gradeChangePolicy.resolveAfterTest(session.getTargetGrade(), passed, score);

        session.setScore(score);
        session.setPassed(passed);
        session.setStatus(SessionStatus.COMPLETED);
        session.setCompletedAt(Instant.now());
        testSessionRepository.save(session);

        Grade previous = profile.getAssignedGrade();
        if (previous != null && previous != assigned) {
            gradeChangePolicy.assertCanChange(profile.getLastGradeChangeAt());
        }
        profile.setAssignedGrade(assigned);
        profile.setTestScore(score);
        profile.setCategoryCode(CategoryCodes.of(
                session.getIndustryCode(), session.getSpecializationCode(), assigned));
        profile.setProfileScore(0.7 * score + 0.3 * averageVector(profile.getSurveyVector()));
        profile.setLastGradeChangeAt(Instant.now());
        profile.touch();
        candidateProfileRepository.save(profile);

        GradeChangeLog log = new GradeChangeLog();
        log.setId(UUID.randomUUID());
        log.setCandidateId(profile.getId());
        log.setFromGrade(previous == null ? null : previous.name());
        log.setToGrade(assigned.name());
        log.setReason(passed ? "TEST_PASSED" : "TEST_FAILED_DOWNGRADE_OR_KEEP");
        log.setChangedAt(Instant.now());
        gradeChangeLogRepository.save(log);

        return session;
    }

    @Transactional(readOnly = true)
    public TestSession get(CurrentUser user, UUID sessionId) {
        CandidateProfile profile = candidateService.requireMine(user);
        TestSession session = testSessionRepository.findById(sessionId)
                .orElseThrow(() -> ApiException.notFound("Тест не найден"));
        if (!session.getCandidateId().equals(profile.getId()) && user.role() != UserRole.ADMIN) {
            throw ApiException.forbidden("Чужой тест");
        }
        return session;
    }

    private static double averageVector(Map<String, Double> vector) {
        if (vector == null || vector.isEmpty()) {
            return 0.0;
        }
        return vector.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }
}
