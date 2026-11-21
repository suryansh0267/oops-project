package com.itsm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Boots the whole application against PostgreSQL.
 * Runs only when ITSM_DB_TESTS=true so `mvn test` works without a database.
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "ITSM_DB_TESTS", matches = "true")
class ItsmApplicationTests {

    @Test
    void contextLoads() {
    }
}
