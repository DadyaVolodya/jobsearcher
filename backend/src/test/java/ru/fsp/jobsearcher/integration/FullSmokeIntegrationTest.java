package ru.fsp.jobsearcher.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
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
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
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
class FullSmokeIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void smokeEndToEnd_fspRanksHigher_chatsSalaryAndProctor() throws Exception {
        mockMvc.perform(get("/api/v1/me")
                        .header("X-User-Email", "smoke-c@example.com")
                        .header("X-User-Role", "CANDIDATE"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/dictionaries/industries")
                        .header("X-User-Email", "smoke-c@example.com")
                        .header("X-User-Role", "CANDIDATE"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/dictionaries/grades")
                        .header("X-User-Email", "smoke-c@example.com")
                        .header("X-User-Role", "CANDIDATE"))
                .andExpect(status().isOk());

        String noFsp = "smoke-nofsp@example.com";
        String withFsp = "smoke-fsp@example.com";
        bootstrapCandidate(noFsp, "No Fsp", "BACKEND", "JUNIOR");
        bootstrapCandidate(withFsp, "Fsp Star", "BACKEND", "JUNIOR");

        mockMvc.perform(post("/api/v1/candidate/fsp")
                        .header("X-User-Email", withFsp)
                        .header("X-User-Role", "CANDIDATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fspParticipantId", "FSP-100500",
                                "title", "Призёр олимпиады",
                                "eventName", "ФСП Code Cup",
                                "place", 2,
                                "points", 250,
                                "fspGrade", "SENIOR"
                        ))))
                .andExpect(status().isOk());

        JsonNode fspSummary = read(mockMvc.perform(get("/api/v1/candidate/fsp")
                        .header("X-User-Email", withFsp)
                        .header("X-User-Role", "CANDIDATE"))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(fspSummary.path("fspGrade").asText()).isEqualTo("SENIOR");
        assertThat(fspSummary.path("totalPoints").asInt()).isEqualTo(250);
        assertThat(fspSummary.path("achievements").isArray()).isTrue();

        UUID needId = createEmployerNeed("smoke-e@example.com", "SmokeCo", "BACKEND", "JUNIOR");

        JsonNode match = read(mockMvc.perform(get("/api/v1/matching/needs/" + needId)
                        .header("X-User-Email", "smoke-e@example.com")
                        .header("X-User-Role", "EMPLOYER"))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(match.path("candidates").size()).isGreaterThanOrEqualTo(2);
        String topId = match.path("candidates").get(0).path("candidateId").asText();
        assertThat(match.path("candidates").get(0).path("fspLinked").asBoolean()).isTrue();
        assertThat(match.path("candidates").get(0).path("fspPoints").asInt()).isEqualTo(250);
        assertThat(match.path("candidates").get(0).path("fspGrade").asText()).isEqualTo("SENIOR");
        assertThat(match.path("candidates").get(0).path("explain").get(0).asText()).contains("SENIOR");
        assertThat(match.path("candidates").get(1).path("fspLinked").asBoolean()).isFalse();

        JsonNode bank = read(mockMvc.perform(get("/api/v1/matching/candidates")
                        .param("specializationCode", "BACKEND")
                        .header("X-User-Email", "smoke-e@example.com")
                        .header("X-User-Role", "EMPLOYER"))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(bank.get(0).path("fspLinked").asBoolean()).isTrue();
        assertThat(bank.get(0).path("fspGrade").asText()).isEqualTo("SENIOR");
        assertThat(bank.get(0).path("fullName").asText()).isEqualTo("Fsp Star");

        JsonNode vacancy = read(mockMvc.perform(post("/api/v1/vacancies")
                        .header("X-User-Email", "smoke-e@example.com")
                        .header("X-User-Role", "EMPLOYER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Backend Java",
                                "description", "Разработка API",
                                "industryCode", "IT",
                                "specializationCode", "BACKEND",
                                "grade", "JUNIOR",
                                "stack", List.of("Java"),
                                "salaryFrom", 120000,
                                "salaryTo", 180000,
                                "publish", true
                        ))))
                .andExpect(status().isOk())
                .andReturn());
        UUID vacancyId = UUID.fromString(vacancy.path("id").asText());

        mockMvc.perform(get("/api/v1/vacancies")
                        .header("X-User-Email", withFsp)
                        .header("X-User-Role", "CANDIDATE"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/vacancies/" + vacancyId + "/applications")
                        .header("X-User-Email", noFsp)
                        .header("X-User-Role", "CANDIDATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"coverLetter\":\"interested\"}"))
                .andExpect(status().isOk());

        JsonNode invitation = read(mockMvc.perform(post("/api/v1/invitations")
                        .header("X-User-Email", "smoke-e@example.com")
                        .header("X-User-Role", "EMPLOYER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "candidateId", topId,
                                "needId", needId.toString(),
                                "vacancyId", vacancyId.toString(),
                                "message", "Оффер с ЗП до чата",
                                "salaryFrom", 160000,
                                "salaryTo", 220000
                        ))))
                .andExpect(status().isOk())
                .andReturn());
        UUID invId = UUID.fromString(invitation.path("id").asText());
        UUID threadId = UUID.fromString(invitation.path("chatThreadId").asText());
        assertThat(invitation.path("salaryFrom").asInt()).isEqualTo(160000);

        mockMvc.perform(post("/api/v1/chats/" + threadId + "/messages")
                        .header("X-User-Email", "smoke-e@example.com")
                        .header("X-User-Role", "EMPLOYER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"Здравствуйте, условия выше\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/chats/" + threadId + "/messages")
                        .header("X-User-Email", withFsp)
                        .header("X-User-Role", "CANDIDATE"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/invitations/" + invId + "/status")
                        .header("X-User-Email", withFsp)
                        .header("X-User-Role", "CANDIDATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DECLINED\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/invitations/" + invId + "/status")
                        .header("X-User-Email", withFsp)
                        .header("X-User-Role", "CANDIDATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DECLINED\",\"declineReason\":\"salary too low\"}"))
                .andExpect(status().isOk());

        JsonNode invList = read(mockMvc.perform(get("/api/v1/invitations")
                        .header("X-User-Email", "smoke-e@example.com")
                        .header("X-User-Role", "EMPLOYER"))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(invList.get(0).path("status").asText()).isEqualTo("DECLINED");
        assertThat(invList.get(0).path("declineReason").asText()).contains("salary");

        // Новый кандидат для proctor fail
        String proctorEmail = "smoke-proctor@example.com";
        bootstrapCandidateUntilTestOpen(proctorEmail, "Proctor", "BACKEND", "JUNIOR");
        JsonNode test = read(mockMvc.perform(post("/api/v1/tests/sessions")
                        .header("X-User-Email", proctorEmail)
                        .header("X-User-Role", "CANDIDATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetGrade\":\"JUNIOR\"}"))
                .andExpect(status().isOk())
                .andReturn());
        UUID testId = UUID.fromString(test.path("id").asText());
        JsonNode failed = read(mockMvc.perform(post("/api/v1/tests/sessions/" + testId + "/proctor")
                        .header("X-User-Email", proctorEmail)
                        .header("X-User-Role", "CANDIDATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"event\":\"TAB_HIDDEN\",\"detail\":\"visibilitychange\"}"))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(failed.path("failReason").asText()).isEqualTo("LEFT_WINDOW");
        assertThat(failed.path("status").asText()).isEqualTo("COMPLETED");
        assertThat(failed.path("passed").asBoolean()).isFalse();
    }

    private void bootstrapCandidate(String email, String name, String spec, String grade) throws Exception {
        bootstrapCandidateUntilTestOpen(email, name, spec, grade);
        JsonNode test = read(mockMvc.perform(post("/api/v1/tests/sessions")
                        .header("X-User-Email", email)
                        .header("X-User-Role", "CANDIDATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetGrade\":\"" + grade + "\"}"))
                .andExpect(status().isOk())
                .andReturn());
        UUID testId = UUID.fromString(test.path("id").asText());
        Map<String, Object> answers = new HashMap<>();
        for (JsonNode item : test.path("items")) {
            String code = item.path("code").asText();
            JsonNode options = item.path("options");
            if (options.isArray() && !options.isEmpty()) {
                answers.put(code, options.get(0).path("code").asText());
            } else {
                answers.put(code, "hash map two sum O(n) pair sum");
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

    private void bootstrapCandidateUntilTestOpen(String email, String name, String spec, String grade) throws Exception {
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
