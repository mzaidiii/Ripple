package org.personal.ripple;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Requires live PostgreSQL instance with pgvector and DB_PASSWORD configured")
class RippleApplicationTests {

    @Test
    void contextLoads() {
    }

}
