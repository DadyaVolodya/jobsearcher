package ru.fsp.jobsearcher.application.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
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
import ru.fsp.jobsearcher.domain.entity.FspAchievement;
import ru.fsp.jobsearcher.domain.enums.Grade;
import ru.fsp.jobsearcher.domain.repository.CandidateProfileRepository;
import ru.fsp.jobsearcher.domain.repository.EmployerNeedRepository;
import ru.fsp.jobsearcher.domain.repository.FspAchievementRepository;
import ru.fsp.jobsearcher.matching.MatchingRanker;

@Service
@RequiredArgsConstructor
public class MatchingService {

    private final EmployerService employerService;
    private final EmployerNeedRepository employerNeedRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final FspAchievementRepository fspAchievementRepository;

    @Transactional(readOnly = true)
    public MatchResponse matchByNeed(CurrentUser user, UUID needId, String stackFilter) {
        EmployerNeed need = employerNeedRepository.findById(needId)
                .orElseThrow(() -> ApiException.notFound("Потребность не найдена"));
        employerService.listNeeds(user).stream()
                .filter(n -> n.getId().equals(needId))
                .findFirst()
                .orElseThrow(() -> ApiException.forbidden("Чужая потребность"));

        String category = need.getIndustryCode() + ":" + need.getSpecializationCode() + ":" + need.getGrade().name();
        List<CandidateProfile> candidates = candidateProfileRepository.searchPublished(
                need.getIndustryCode(), need.getSpecializationCode(), need.getGrade(), null);

        List<MatchingRanker.RankedCandidate> ranked = new ArrayList<>();
        for (CandidateProfile c : candidates) {
            if (stackFilter != null && !stackFilter.isBlank()) {
                boolean ok = c.getStack().stream().anyMatch(s -> s.equalsIgnoreCase(stackFilter));
                if (!ok) {
                    continue;
                }
            }
            int fspPoints = fspAchievementRepository.findByCandidateId(c.getId()).stream()
                    .mapToInt(FspAchievement::getPoints).sum();
            double vectorScore = MatchingRanker.cosine(need.getNeedVector(), c.getSurveyVector());
            double score = MatchingRanker.totalScore(
                    need.getNeedVector(), c.getSurveyVector(), need.getStack(), c.getStack(), c.getTestScore(), fspPoints);
            List<String> explain = MatchingRanker.explain(
                    c.getSpecializationCode(),
                    c.getAssignedGrade() == null ? "?" : c.getAssignedGrade().name(),
                    need.getStack(),
                    c.getStack(),
                    c.getTestScore(),
                    fspPoints,
                    vectorScore
            );
            ranked.add(new MatchingRanker.RankedCandidate(c.getId().toString(), c.getCategoryCode(), score, explain));
        }
        ranked = MatchingRanker.sort(ranked);

        Map<String, List<MatchingRanker.RankedCandidate>> byCategory = new LinkedHashMap<>();
        byCategory.put(category, ranked);
        return new MatchResponse(needId, category, ranked, byCategory);
    }

    @Transactional(readOnly = true)
    public List<CandidatePublicView> searchBank(
            String industry,
            String specialization,
            String grade,
            String category,
            String stack,
            boolean requireFsp
    ) {
        Grade g = grade == null || grade.isBlank() ? null : Grade.from(grade);
        List<CandidateProfile> list = candidateProfileRepository.searchPublished(industry, specialization, g, category);
        List<CandidatePublicView> result = new ArrayList<>();
        for (CandidateProfile c : list) {
            int fspPoints = fspAchievementRepository.findByCandidateId(c.getId()).stream()
                    .mapToInt(FspAchievement::getPoints).sum();
            if (requireFsp && fspPoints <= 0) {
                continue;
            }
            if (stack != null && !stack.isBlank()
                    && c.getStack().stream().noneMatch(s -> s.equalsIgnoreCase(stack))) {
                continue;
            }
            result.add(CandidatePublicView.from(c, fspPoints, false));
        }
        result.sort((a, b) -> Double.compare(b.profileScore(), a.profileScore()));
        return result;
    }

    public record MatchResponse(
            UUID needId,
            String primaryCategory,
            List<MatchingRanker.RankedCandidate> candidates,
            Map<String, List<MatchingRanker.RankedCandidate>> categories
    ) {
    }

    public record CandidatePublicView(
            UUID id,
            String fullName,
            String categoryCode,
            String industryCode,
            String specializationCode,
            String grade,
            List<String> stack,
            double testScore,
            double profileScore,
            int fspPoints,
            String city,
            String email,
            String phone
    ) {
        static CandidatePublicView from(CandidateProfile c, int fspPoints, boolean revealContacts) {
            return new CandidatePublicView(
                    c.getId(),
                    c.getFullName(),
                    c.getCategoryCode(),
                    c.getIndustryCode(),
                    c.getSpecializationCode(),
                    c.getAssignedGrade() == null ? null : c.getAssignedGrade().name(),
                    c.getStack(),
                    c.getTestScore(),
                    c.getProfileScore(),
                    fspPoints,
                    c.getCity(),
                    revealContacts ? null : null,
                    revealContacts ? null : null
            );
        }
    }
}
