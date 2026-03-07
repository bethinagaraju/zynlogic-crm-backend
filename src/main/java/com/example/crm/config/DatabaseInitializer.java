package com.example.crm.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        String sql = "CREATE TABLE IF NOT EXISTS email_lead ("
                + "id VARCHAR(255) PRIMARY KEY,"
                + "timestamp_created TIMESTAMP,"
                + "timestamp_email TIMESTAMP,"
                + "organization_id VARCHAR(255),"
                + "eaccount VARCHAR(255),"
                + "from_address_email VARCHAR(255),"
                + "campaign_id VARCHAR(255),"
                + "lead VARCHAR(255),"
                + "ue_type INTEGER,"
                + "step VARCHAR(255),"
                + "is_unread INTEGER,"
                + "ai_interest_value INTEGER,"
                + "is_focused INTEGER,"
                + "i_status INTEGER,"
                + "thread_id VARCHAR(255)"
                + ")";

        jdbcTemplate.execute(sql);
    }
}
