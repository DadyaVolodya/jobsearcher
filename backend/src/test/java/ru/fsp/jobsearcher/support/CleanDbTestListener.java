package ru.fsp.jobsearcher.support;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

/**
 * Truncates business tables between integration tests when using shared Postgres.
 */
public class CleanDbTestListener implements TestExecutionListener {

    @Override
    public void beforeTestMethod(TestContext testContext) {
        JdbcTemplate jdbc = testContext.getApplicationContext().getBean(JdbcTemplate.class);
        jdbc.execute("""
            TRUNCATE TABLE
              vacancy_application, vacancy, invitation, fsp_achievement, grade_change_log,
              test_session, survey_session, employer_need, stored_file,
              candidate_profile, employer_profile, user_consent, app_user
            RESTART IDENTITY CASCADE
            """);
    }
}
