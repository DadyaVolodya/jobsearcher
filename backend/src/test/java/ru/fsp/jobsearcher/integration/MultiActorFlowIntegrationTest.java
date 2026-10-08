package ru.fsp.jobsearcher.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import ru.fsp.jobsearcher.infrastructure.security.TestSecurityConfig;
import ru.fsp.jobsearcher.support.CleanDbTestListener;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@TestExecutionListeners(listeners = {
        DependencyInjectionTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class,
        TransactionalTestExecutionListener.class,
        CleanDbTestListener.class
}, mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
class MultiActorFlowIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void twoCandidatesTwoEmployersInvitationVisibility() throws Exception {
        bootstrapCandidate("c1@example.com", "Alice", "BACKEND", "JUNIOR");
        bootstrapCandidate("c2@example.com", "Bob", "FRONTEND", "JUNIOR");

        UUID need1 = createEmployerNeed("e1@example.com", "Acme", "BACKEND", "JUNIOR");
        UUID need2 = createEmployerNeed("e2@example.com", "Beta", "FRONTEND", "JUNIOR");

        JsonNode match1 = read(mockMvc.perform(get("/api/v1/matching/needs/" + need1)
                        .header("X-User-Email", "e1@example.com")
                        .header("X-User-Role", "EMPLOYER"))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(match1.path("candidates").isArray()).isTrue();

        JsonNode bank = read(mockMvc.perform(get("/api/v1/matching/candidates")
                        .param("specializationCode", "BACKEND")
                        .header("X-User-Email", "e1@example.com")
                        .header("X-User-Role", "EMPLOYER"))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(bank.isArray()).isTrue();
        assertThat(bank.size()).isGreaterThanOrEqualTo(1);
        String candidateId = bank.get(0).path("id").asText();

        JsonNode invitation = read(mockMvc.perform(post("/api/v1/invitations")
                        .header("X-User-Email", "e1@example.com")
                        .header("X-User-Role", "EMPLOYER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "candidateId", candidateId,
                                "needId", need1.toString(),
                                "message", "Оффер backend",
                                "salaryFrom", 150000,
                                "salaryTo", 200000
                        ))))
                .andExpect(status().isOk())
                .andReturn());
        UUID invId = UUID.fromString(invitation.path("id").asText());
        assertThat(invitation.path("chatThreadId").asText()).isNotBlank();

        JsonNode beforeAccept = read(mockMvc.perform(get("/api/v1/invitations")
                        .header("X-User-Email", "e1@example.com")
                        .header("X-User-Role", "EMPLOYER"))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(beforeAccept.get(0).path("candidateEmail").isNull()).isTrue();

        mockMvc.perform(post("/api/v1/invitations/" + invId + "/status")
                        .header("X-User-Email", "c1@example.com")
                        .header("X-User-Role", "CANDIDATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACCEPTED\"}"))
                .andExpect(status().isOk());

        JsonNode afterAccept = read(mockMvc.perform(get("/api/v1/invitations")
                        .header("X-User-Email", "e1@example.com")
                        .header("X-User-Role", "EMPLOYER"))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(afterAccept.get(0).path("candidateEmail").asText()).contains("c1@example.com");

        mockMvc.perform(get("/api/v1/matching/needs/" + need2)
                        .header("X-User-Email", "e1@example.com")
                        .header("X-User-Role", "EMPLOYER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void candidateCannotSeeOtherCandidateProfileDataViaInvitations() throws Exception {
        bootstrapCandidate("c3@example.com", "Carl", "BACKEND", "MIDDLE");
        createEmployerNeed("e3@example.com", "Gamma", "BACKEND", "MIDDLE");
        JsonNode bank = read(mockMvc.perform(get("/api/v1/matching/candidates")
                        .param("specializationCode", "BACKEND")
                        .header("X-User-Email", "e3@example.com")
                        .header("X-User-Role", "EMPLOYER"))
                .andExpect(status().isOk())
                .andReturn());
        String candidateId = bank.get(0).path("id").asText();
        JsonNode inv = read(mockMvc.perform(post("/api/v1/invitations")
                        .header("X-User-Email", "e3@example.com")
                        .header("X-User-Role", "EMPLOYER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "candidateId", candidateId,
                                "message", "hi",
                                "salaryFrom", 100000,
                                "salaryTo", 120000
                        ))))
                .andExpect(status().isOk())
                .andReturn());
        mockMvc.perform(post("/api/v1/invitations/" + inv.path("id").asText() + "/status")
                        .header("X-User-Email", "c4@example.com")
                        .header("X-User-Role", "CANDIDATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACCEPTED\"}"))
                .andExpect(status().isForbidden());
    }

    private void bootstrapCandidate(String email, String name, String spec, String grade) throws Exception {
        mockMvc.perform(put("/api/v1/candidate/profile")
                        .header("X-User-Email", email)
                        .header("X-User-Role", "CANDIDATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fullName", name,
                                "phone", "+79990001122",
                                "city", "Москва",
                                "stack", List.of("Java", "Spring"),
                                "industryCode", "IT",
                                "specializationCode", spec,
                                "claimedGrade", grade
                        ))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/consents")
                        .header("X-User-Email", email)
                        .header("X-User-Role", "CANDIDATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"PERSONAL_DATA_PROCESSING\",\"granted\":true}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/consents")
                        .header("X-User-Email", email)
                        .header("X-User-Role", "CANDIDATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"PROFILE_PUBLICATION\",\"granted\":true}"))
                .andExpect(status().isOk());

        JsonNode session = read(mockMvc.perform(post("/api/v1/surveys/sessions")
                        .header("X-User-Email", email)
                        .header("X-User-Role", "CANDIDATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionnaireCode\":\"IT_CANDIDATE\"}"))
                .andExpect(status().isOk())
                .andReturn());
        UUID surveyId = UUID.fromString(session.path("id").asText());
        mockMvc.perform(post("/api/v1/surveys/sessions/" + surveyId + "/answers")
                        .header("X-User-Email", email)
                        .header("X-User-Role", "CANDIDATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "IT_C_STACK", "JAVA",
                                "IT_C_ARCH", "4",
                                "IT_C_DB", "4",
                                "IT_C_TESTS", "ALWAYS",
                                "IT_C_TEAM", "REMOTE",
                                "IT_C_SOFT", "4",
                                "IT_C_SPEC", spec,
                                "IT_C_COMPLEXITY", "4"
                        ))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/surveys/sessions/" + surveyId + "/complete")
                        .header("X-User-Email", email)
                        .header("X-User-Role", "CANDIDATE"))
                .andExpect(status().isOk());

        JsonNode test = read(mockMvc.perform(post("/api/v1/tests/sessions")
                        .header("X-User-Email", email)
                        .header("X-User-Role", "CANDIDATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetGrade\":\"" + grade + "\"}"))
                .andExpect(status().isOk())
                .andReturn());
        UUID testId = UUID.fromString(test.path("id").asText());
        Map<String, Object> answers = new java.util.HashMap<>();
        for (JsonNode item : test.path("items")) {
            String code = item.path("code").asText();
            JsonNode options = item.path("options");
            if (options.isArray() && !options.isEmpty()) {
                answers.put(code, options.get(0).path("code").asText());
            } else {
                answers.put(code, "idempotency key deduplication transaction status endpoint");
            }
        }
        mockMvc.perform(post("/api/v1/tests/sessions/" + testId + "/submit")
                        .header("X-User-Email", email)
                        .header("X-User-Role", "CANDIDATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(answers)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/candidate/publish")
                        .header("X-User-Email", email)
                        .header("X-User-Role", "CANDIDATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"published\":true}"))
                .andExpect(status().isOk());
    }

    private UUID createEmployerNeed(String email, String company, String spec, String grade) throws Exception {
        mockMvc.perform(put("/api/v1/employer/profile")
                        .header("X-User-Email", email)
                        .header("X-User-Role", "EMPLOYER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "companyName", company,
                                "description", "IT company",
                                "contactEmail", email
                        ))))
                .andExpect(status().isOk());
        JsonNode need = read(mockMvc.perform(post("/api/v1/employer/needs")
                        .header("X-User-Email", email)
                        .header("X-User-Role", "EMPLOYER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Need " + spec,
                                "description", "Team needs " + spec,
                                "industryCode", "IT",
                                "specializationCode", spec,
                                "grade", grade,
                                "stack", List.of("Java"),
                                "salaryFrom", 150000,
                                "salaryTo", 250000,
                                "needVector", Map.of("architecture", 0.8, "databases", 0.7, "stack_affinity", 1.0)
                        ))))
                .andExpect(status().isOk())
                .andReturn());
        return UUID.fromString(need.path("id").asText());
    }

    private JsonNode read(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
