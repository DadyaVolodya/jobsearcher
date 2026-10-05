package ru.fsp.jobsearcher.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.fsp.jobsearcher.api.common.ApiException;
import ru.fsp.jobsearcher.application.security.CurrentUser;
import ru.fsp.jobsearcher.domain.entity.CandidateProfile;
import ru.fsp.jobsearcher.domain.entity.StoredFile;
import ru.fsp.jobsearcher.domain.repository.StoredFileRepository;
import ru.fsp.jobsearcher.infrastructure.llm.LlmGateway;
import ru.fsp.jobsearcher.infrastructure.storage.StoragePort;

@Service
@RequiredArgsConstructor
public class ResumeParseService {

    private final StoragePort storagePort;
    private final StoredFileRepository storedFileRepository;
    private final CandidateService candidateService;
    private final LlmGateway llmGateway;
    private final ObjectMapper objectMapper;

    @Transactional
    public CandidateProfile uploadAndParse(CurrentUser user, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badState("Файл пуст");
        }
        try {
            String key = user.userId() + "/resumes/" + UUID.randomUUID() + "-" + file.getOriginalFilename();
            StoragePort.StoredObject stored = storagePort.store(
                    key, file.getInputStream(), file.getSize(), file.getContentType());
            StoredFile meta = new StoredFile();
            meta.setId(UUID.randomUUID());
            meta.setOwnerUserId(user.userId());
            meta.setOriginalName(file.getOriginalFilename());
            meta.setContentType(file.getContentType());
            meta.setStorageKey(stored.key());
            meta.setSizeBytes(stored.sizeBytes());
            meta.setPurpose("RESUME");
            meta.setCreatedAt(java.time.Instant.now());
            storedFileRepository.save(meta);

            String text = extractText(file);
            ParsedResume parsed = parseWithLlmOrHeuristic(text);
            return candidateService.updateProfile(user, new CandidateService.UpdateRequest(
                    parsed.fullName(),
                    parsed.phone(),
                    parsed.city(),
                    parsed.about(),
                    parsed.stack(),
                    parsed.softSkills(),
                    parsed.experienceYears(),
                    text,
                    null,
                    parsed.grade(),
                    null,
                    null,
                    null
            ));
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw ApiException.badState("Не удалось разобрать резюме: " + ex.getMessage());
        }
    }

    private String extractText(MultipartFile file) throws Exception {
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (name.endsWith(".pdf") || "application/pdf".equals(file.getContentType())) {
            try (InputStream in = file.getInputStream(); PDDocument doc = Loader.loadPDF(in.readAllBytes())) {
                return new PDFTextStripper().getText(doc);
            }
        }
        return new String(file.getBytes());
    }

    private ParsedResume parseWithLlmOrHeuristic(String text) {
        try {
            String json = llmGateway.chat(
                    "Ты парсер резюме. Верни только JSON без markdown: "
                            + "{\"fullName\",\"phone\",\"city\",\"about\",\"stack\":[],\"softSkills\":[],"
                            + "\"experienceYears\":number,\"grade\":\"JUNIOR|MIDDLE|SENIOR|INTERN\"}",
                    text.length() > 8000 ? text.substring(0, 8000) : text
            );
            String cleaned = json.trim();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("^```json|^```|```$", "").trim();
            }
            JsonNode node = objectMapper.readTree(cleaned);
            List<String> stack = new ArrayList<>();
            if (node.path("stack").isArray()) {
                node.path("stack").forEach(n -> stack.add(n.asText()));
            }
            List<String> soft = new ArrayList<>();
            if (node.path("softSkills").isArray()) {
                node.path("softSkills").forEach(n -> soft.add(n.asText()));
            }
            BigDecimal years = node.path("experienceYears").isNumber()
                    ? BigDecimal.valueOf(node.path("experienceYears").asDouble())
                    : null;
            return new ParsedResume(
                    textOrNull(node, "fullName"),
                    textOrNull(node, "phone"),
                    textOrNull(node, "city"),
                    textOrNull(node, "about"),
                    stack,
                    soft,
                    years,
                    textOrNull(node, "grade")
            );
        } catch (Exception ex) {
            return heuristic(text);
        }
    }

    private ParsedResume heuristic(String text) {
        List<String> stack = new ArrayList<>();
        for (String token : List.of("Java", "Spring", "Python", "React", "PostgreSQL", "Docker", "Kubernetes")) {
            if (text.toLowerCase().contains(token.toLowerCase())) {
                stack.add(token);
            }
        }
        String firstLine = text.lines().findFirst().orElse("Кандидат");
        return new ParsedResume(firstLine.trim(), null, null,
                text.length() > 400 ? text.substring(0, 400) : text,
                stack, List.of(), null, "JUNIOR");
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode v = node.path(field);
        return v.isMissingNode() || v.asText().isBlank() ? null : v.asText();
    }

    private record ParsedResume(
            String fullName,
            String phone,
            String city,
            String about,
            List<String> stack,
            List<String> softSkills,
            BigDecimal experienceYears,
            String grade
    ) {
    }
}
