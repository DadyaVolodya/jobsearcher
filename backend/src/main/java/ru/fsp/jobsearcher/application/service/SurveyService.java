package ru.fsp.jobsearcher.application.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.fsp.jobsearcher.api.common.ApiException;
import ru.fsp.jobsearcher.application.security.CurrentUser;
import ru.fsp.jobsearcher.domain.entity.CandidateProfile;
import ru.fsp.jobsearcher.domain.entity.EmployerNeed;
import ru.fsp.jobsearcher.domain.entity.Questionnaire;
import ru.fsp.jobsearcher.domain.entity.SurveyQuestion;
import ru.fsp.jobsearcher.domain.entity.SurveySession;
import ru.fsp.jobsearcher.domain.enums.SessionStatus;
import ru.fsp.jobsearcher.domain.enums.UserRole;
import ru.fsp.jobsearcher.domain.repository.CandidateProfileRepository;
import ru.fsp.jobsearcher.domain.repository.EmployerNeedRepository;
import ru.fsp.jobsearcher.domain.repository.QuestionnaireRepository;
import ru.fsp.jobsearcher.domain.repository.SurveyQuestionRepository;
import ru.fsp.jobsearcher.domain.repository.SurveySessionRepository;
import ru.fsp.jobsearcher.survey.scoring.SurveyScoringEngine;

@Service
@RequiredArgsConstructor
public class SurveyService {

    private final QuestionnaireRepository questionnaireRepository;
    private final SurveyQuestionRepository surveyQuestionRepository;
    private final SurveySessionRepository surveySessionRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final EmployerNeedRepository employerNeedRepository;
    private final EmployerService employerService;

    @Transactional(readOnly = true)
    public List<Questionnaire> list(String audience, String industryCode) {
        if (industryCode != null && !industryCode.isBlank()) {
            return questionnaireRepository.findByActiveTrueAndAudienceAndIndustryCode(audience, industryCode);
        }
        return questionnaireRepository.findByActiveTrueAndAudience(audience);
    }

    @Transactional(readOnly = true)
    public List<SurveyQuestion> questions(UUID questionnaireId) {
        return surveyQuestionRepository.findByQuestionnaireIdOrderBySortOrderAsc(questionnaireId);
    }

    @Transactional
    public SurveySession start(CurrentUser user, String questionnaireCode) {
        Questionnaire q = questionnaireRepository.findByCode(questionnaireCode)
                .orElseThrow(() -> ApiException.notFound("Анкета не найдена"));
        SurveySession session = new SurveySession();
        session.setId(UUID.randomUUID());
        session.setUserId(user.userId());
        session.setQuestionnaireId(q.getId());
        session.setStatus(SessionStatus.IN_PROGRESS);
        session.setAnswers(new HashMap<>());
        session.setResultVector(new HashMap<>());
        session.setStartedAt(Instant.now());
        return surveySessionRepository.save(session);
    }

    @Transactional
    public SurveySession answer(CurrentUser user, UUID sessionId, Map<String, Object> answers) {
        SurveySession session = getOwnedSession(user, sessionId);
        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw ApiException.badState("Сессия анкеты уже завершена");
        }
        Map<String, Object> merged = new HashMap<>(session.getAnswers());
        merged.putAll(answers);
        session.setAnswers(merged);
        return surveySessionRepository.save(session);
    }

    @Transactional
    public SurveySession complete(CurrentUser user, UUID sessionId, UUID employerNeedId) {
        SurveySession session = getOwnedSession(user, sessionId);
        List<SurveyQuestion> questions = surveyQuestionRepository
                .findByQuestionnaireIdOrderBySortOrderAsc(session.getQuestionnaireId());
        Map<String, Double> vector = SurveyScoringEngine.score(questions, session.getAnswers());
        session.setResultVector(vector);
        session.setStatus(SessionStatus.COMPLETED);
        session.setCompletedAt(Instant.now());
        surveySessionRepository.save(session);

        Questionnaire questionnaire = questionnaireRepository.findById(session.getQuestionnaireId())
                .orElseThrow();
        if ("CANDIDATE".equals(questionnaire.getAudience()) && user.role() == UserRole.CANDIDATE) {
            CandidateProfile profile = candidateProfileRepository.findByUserId(user.userId())
                    .orElseThrow(() -> ApiException.notFound("Профиль кандидата не найден"));
            profile.setSurveyVector(vector);
            profile.setIndustryCode(questionnaire.getIndustryCode());
            Object spec = session.getAnswers().get("IT_C_SPEC");
            if (spec != null) {
                profile.setSpecializationCode(String.valueOf(spec));
            }
            profile.touch();
            candidateProfileRepository.save(profile);
        } else if ("EMPLOYER".equals(questionnaire.getAudience()) && employerNeedId != null) {
            EmployerNeed need = employerNeedRepository.findById(employerNeedId)
                    .orElseThrow(() -> ApiException.notFound("Потребность не найдена"));
            EmployerNeed owned = employerService.listNeeds(user).stream()
                    .filter(n -> n.getId().equals(need.getId()))
                    .findFirst()
                    .orElseThrow(() -> ApiException.forbidden("Чужая потребность"));
            owned.setNeedVector(vector);
            owned.setUpdatedAt(Instant.now());
            employerNeedRepository.save(owned);
        }
        return session;
    }

    private SurveySession getOwnedSession(CurrentUser user, UUID sessionId) {
        SurveySession session = surveySessionRepository.findById(sessionId)
                .orElseThrow(() -> ApiException.notFound("Сессия не найдена"));
        if (!session.getUserId().equals(user.userId()) && user.role() != UserRole.ADMIN) {
            throw ApiException.forbidden("Чужая сессия анкеты");
        }
        return session;
    }
}
