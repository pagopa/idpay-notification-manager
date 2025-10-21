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
        "notification.manager.markdown.ok-cta=MARKDOWN_OK_CTA",
        "notification.manager.markdown.double-new-line=  ",
        "notification.manager.markdown.ok-bel=MARKDOWN_OK",
        "notification.manager.markdown.ok-partial-bel=MARKDOWN_OK_PARTIAL",
        "notification.manager.subject.ok-bel=SUBJECT_OK",
        "notification.manager.subject.ok-partial-bel=SUBJECT_OK_PARTIAL",
        "notification.manager.subject.joined-bel=SUBJECT_JOINED_BEL",
        "notification.manager.subject.ko-thanks-bel=SUBJECT_KO_THANKS_BEL",
        "notification.manager.subject.ko-generic-bel=SUBJECT_KO_GENERIC_BEL",
        "notification.manager.markdown.joined-bel=MARKDOWN_JOINED_BEL",
        "notification.manager.markdown.ko-thanks-bel=MARKDOWN_KO_THANKS_BEL",
        "notification.manager.markdown.ko-generic-bel=MARKDOWN_KO_GENERIC_BEL"
})
@EnableConfigurationProperties(value = NotificationProperties.class)
class NotificationPropertiesTest {

    @Autowired
    private NotificationProperties properties;

    @Test
    void propertiesShouldBeBoundCorrectly(){
        Assertions.assertNotNull(properties.getSubject());
        Assertions.assertEquals("SUBJECT_OK", properties.getSubject().getOkBel());
        Assertions.assertEquals("SUBJECT_OK_PARTIAL", properties.getSubject().getOkPartialBel());

        Assertions.assertNotNull(properties.getMarkdown());
        Assertions.assertEquals("MARKDOWN_OK_CTA", properties.getMarkdown().getOkCta());
        Assertions.assertNotNull(properties.getMarkdown().getDoubleNewLine());
        Assertions.assertEquals("MARKDOWN_OK", properties.getMarkdown().getOkBel());
        Assertions.assertEquals("MARKDOWN_OK_PARTIAL", properties.getMarkdown().getOkPartialBel());

        Assertions.assertEquals("SUBJECT_KO_THANKS_BEL", properties.getSubject().getKoThanksBel());
        Assertions.assertEquals("SUBJECT_KO_GENERIC_BEL", properties.getSubject().getKoGenericBel());
        Assertions.assertEquals("SUBJECT_JOINED_BEL", properties.getSubject().getJoinedBel());

        Assertions.assertEquals("MARKDOWN_JOINED_BEL", properties.getMarkdown().getJoinedBel());
        Assertions.assertEquals("MARKDOWN_KO_THANKS_BEL", properties.getMarkdown().getKoThanksBel());
        Assertions.assertEquals("MARKDOWN_KO_GENERIC_BEL", properties.getMarkdown().getKoGenericBel());



    }
  
}