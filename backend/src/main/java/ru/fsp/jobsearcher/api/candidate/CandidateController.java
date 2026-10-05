package ru.fsp.jobsearcher.api.candidate;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.fsp.jobsearcher.application.security.SecurityUtils;
import ru.fsp.jobsearcher.application.service.CandidateService;
import ru.fsp.jobsearcher.application.service.ResumeParseService;
import ru.fsp.jobsearcher.domain.entity.CandidateProfile;
import ru.fsp.jobsearcher.domain.entity.FspAchievement;

@RestController
@RequestMapping("/api/v1/candidate")
@RequiredArgsConstructor
@Tag(name = "Candidate LK")
public class CandidateController {

    private final SecurityUtils securityUtils;
    private final CandidateService candidateService;
    private final ResumeParseService resumeParseService;

    @GetMapping("/profile")
    public CandidateProfile profile() {
        return candidateService.requireMine(securityUtils.requireCurrentUser());
    }

    @PutMapping("/profile")
    public CandidateProfile update(@RequestBody CandidateService.UpdateRequest req) {
        return candidateService.updateProfile(securityUtils.requireCurrentUser(), req);
    }

    @PostMapping("/publish")
    public CandidateProfile publish(@RequestBody Map<String, Boolean> body) {
        boolean published = Boolean.TRUE.equals(body.get("published"));
        return candidateService.publish(securityUtils.requireCurrentUser(), published);
    }

    @PostMapping(value = "/resume", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CandidateProfile uploadResume(@RequestPart("file") MultipartFile file) {
        return resumeParseService.uploadAndParse(securityUtils.requireCurrentUser(), file);
    }

    @GetMapping("/fsp")
    public List<FspAchievement> fsp() {
        return candidateService.achievements(candidateService.requireMine(securityUtils.requireCurrentUser()).getId());
    }

    @PostMapping("/fsp")
    public FspAchievement linkFsp(@RequestBody Map<String, Object> body) {
        return candidateService.linkFspStub(
                securityUtils.requireCurrentUser(),
                String.valueOf(body.get("fspParticipantId")),
                body.get("title") == null ? null : String.valueOf(body.get("title")),
                body.get("eventName") == null ? null : String.valueOf(body.get("eventName")),
                body.get("place") == null ? null : ((Number) body.get("place")).intValue(),
                body.get("points") == null ? 0 : ((Number) body.get("points")).intValue()
        );
    }
}
