package it.gov.pagopa.notification.manager.service.io;

import it.gov.pagopa.notification.manager.config.ProfilesNotificationProperties;
import it.gov.pagopa.notification.manager.enums.NotificationEvent;
import it.gov.pagopa.notification.manager.model.EmailNotificationContent;
import it.gov.pagopa.notification.manager.service.ProfilesNotificationCatalog;
import it.gov.pagopa.notification.manager.service.email.EmailNotificationContentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static it.gov.pagopa.notification.manager.enums.NotificationEvent.ONBOARDING_OK;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EmailNotificationContentServiceTest {

    private static final EmailNotificationContent DEFAULT_CONTENT =
            new EmailNotificationContent("Default subject", "Email_DEFAULT/EsitoOk");
    private static final EmailNotificationContent CUSTOM_CONTENT =
            new EmailNotificationContent("Custom subject", "Email_CUSTOM/EsitoOk");

    private ProfilesNotificationProperties properties;
    private EmailNotificationContentService service;

    @BeforeEach
    void setUp() {
        properties = new ProfilesNotificationProperties();
        ProfilesNotificationProperties.ProfileConfig defaultProfile = new ProfilesNotificationProperties.ProfileConfig();
        defaultProfile.getEmailEvents().put(ONBOARDING_OK, DEFAULT_CONTENT);
        ProfilesNotificationProperties.ProfileConfig customProfile = new ProfilesNotificationProperties.ProfileConfig();
        customProfile.getEmailEvents().put(ONBOARDING_OK, CUSTOM_CONTENT);
        properties.getProfiles().put("DEFAULT", defaultProfile);
        properties.getProfiles().put("CUSTOM", customProfile);
        service = new EmailNotificationContentService(new ProfilesNotificationCatalog(properties));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "DEFAULT"})
    void bothLookupsReturnConfiguredDefaultContent(String profile) {
        assertEquals(Optional.of(DEFAULT_CONTENT), service.resolveProfileAndFind(profile, ONBOARDING_OK));
        assertEquals(Optional.of(DEFAULT_CONTENT), service.resolveProfileAndFindWithDefaultFallback(profile, ONBOARDING_OK));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "DEFAULT"})
    void fallbackLookupReturnsPlaceholderForMissingDefaultEvent(String profile) {
        properties.getProfiles().get("DEFAULT").getEmailEvents().clear();

        assertEquals(Optional.empty(), service.resolveProfileAndFind(profile, ONBOARDING_OK));
        assertEquals(Optional.of(new EmailNotificationContent("Fallback subject", "Email_null/ONBOARDING_OK")),
                service.resolveProfileAndFindWithDefaultFallback(profile, ONBOARDING_OK));
    }

    @ParameterizedTest
    @EnumSource(NotificationEvent.class)
    void fallbackTemplateContainsTheActualEvent(NotificationEvent event) {
        properties.getProfiles().get("DEFAULT").getEmailEvents().clear();

        assertEquals(Optional.of(new EmailNotificationContent("Fallback subject", "Email_null/" + event.name())),
                service.resolveProfileAndFindWithDefaultFallback("DEFAULT", event));
    }

    @Test
    void bothLookupsReturnCustomContent() {
        assertEquals(Optional.of(CUSTOM_CONTENT), service.resolveProfileAndFind("CUSTOM", ONBOARDING_OK));
        assertEquals(Optional.of(CUSTOM_CONTENT), service.resolveProfileAndFindWithDefaultFallback("CUSTOM", ONBOARDING_OK));
    }

    @Test
    void missingCustomEventDoesNotFallBackToDefault() {
        properties.getProfiles().get("CUSTOM").getEmailEvents().clear();

        assertEquals(Optional.empty(), service.resolveProfileAndFind("CUSTOM", ONBOARDING_OK));
        assertEquals(Optional.empty(), service.resolveProfileAndFindWithDefaultFallback("CUSTOM", ONBOARDING_OK));
    }

    @Test
    void unknownProfileDoesNotFallBackToDefault() {
        assertEquals(Optional.empty(), service.resolveProfileAndFind("UNKNOWN", ONBOARDING_OK));
        assertEquals(Optional.empty(), service.resolveProfileAndFindWithDefaultFallback("UNKNOWN", ONBOARDING_OK));
    }
}
