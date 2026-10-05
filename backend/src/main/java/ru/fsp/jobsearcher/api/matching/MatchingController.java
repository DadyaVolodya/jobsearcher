package ru.fsp.jobsearcher.api.matching;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.fsp.jobsearcher.application.security.SecurityUtils;
import ru.fsp.jobsearcher.application.service.MatchingService;

@RestController
@RequestMapping("/api/v1/matching")
@RequiredArgsConstructor
@Tag(name = "Matching")
public class MatchingController {

    private final SecurityUtils securityUtils;
    private final MatchingService matchingService;

    @GetMapping("/needs/{needId}")
    public MatchingService.MatchResponse byNeed(
            @PathVariable UUID needId,
            @RequestParam(required = false) String stack
    ) {
        return matchingService.matchByNeed(securityUtils.requireCurrentUser(), needId, stack);
    }

    @GetMapping("/candidates")
    public List<MatchingService.CandidatePublicView> bank(
            @RequestParam(required = false) String industryCode,
            @RequestParam(required = false) String specializationCode,
            @RequestParam(required = false) String grade,
            @RequestParam(required = false) String categoryCode,
            @RequestParam(required = false) String stack,
            @RequestParam(defaultValue = "false") boolean requireFsp
    ) {
        return matchingService.searchBank(industryCode, specializationCode, grade, categoryCode, stack, requireFsp);
    }
}
