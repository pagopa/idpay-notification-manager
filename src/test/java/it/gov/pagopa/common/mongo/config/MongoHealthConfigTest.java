package it.gov.pagopa.common.mongo.config;

import it.gov.pagopa.common.config.CustomMongoHealthIndicator;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class MongoHealthConfigTest {

    private final MongoHealthConfig config = new MongoHealthConfig();

    @Test
    void customMongoHealthIndicator_shouldCreateBean() {

        MongoTemplate mongoTemplate = mock(MongoTemplate.class);

        CustomMongoHealthIndicator indicator =
                config.customMongoHealthIndicator(mongoTemplate);

        assertNotNull(indicator);
    }

}
