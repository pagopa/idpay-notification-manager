package it.gov.pagopa.notification.manager.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;


@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
        "rest-client.notification.email-notification.subject.ok=SUBJECT_OK",
        "rest-client.notification.email-notification.subject.partial=SUBJECT_PARTIAL_OK"
})
@EnableConfigurationProperties(value = EmailNotificationProperties.class)
class EmailNotificationPropertiesTest {

    @Autowired
    private EmailNotificationProperties properties;

    @Test
    void propertiesShouldBeBoundCorrectly() {
        Assertions.assertNotNull(properties.getSubject());
        Assertions.assertEquals("SUBJECT_OK", properties.getSubject().getOk());
        Assertions.assertEquals("SUBJECT_PARTIAL_OK", properties.getSubject().getPartial());
    }
}