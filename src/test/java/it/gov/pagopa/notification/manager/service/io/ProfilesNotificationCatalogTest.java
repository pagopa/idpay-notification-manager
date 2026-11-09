package it.gov.pagopa.notification.manager.service.io;

import it.gov.pagopa.notification.manager.config.ProfilesNotificationProperties;
import it.gov.pagopa.notification.manager.enums.NotificationEvent;
import it.gov.pagopa.notification.manager.model.NotificationContent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ProfilesNotificationCatalogTest {

    private ProfilesNotificationProperties properties;
    private ProfilesNotificationCatalog catalog;

    @BeforeEach
    void setUp() {
        properties = new ProfilesNotificationProperties();

        ProfilesNotificationProperties.ProfileConfig defaultProfileConfig =
                new ProfilesNotificationProperties.ProfileConfig();
        ProfilesNotificationProperties.ProfileConfigVariables defaultVariables =
                new ProfilesNotificationProperties.ProfileConfigVariables();
        defaultVariables.setAssistedLink("https://default.link");
        defaultProfileConfig.setVariables(defaultVariables);

        Map<NotificationEvent, NotificationContent> defaultEvents = new EnumMap<>(NotificationEvent.class);
        defaultEvents.put(NotificationEvent.ONBOARDING_OK, new NotificationContent("Default OK", "Markdown OK"));
        defaultProfileConfig.setEvents(defaultEvents);

        ProfilesNotificationProperties.ProfileConfig customProfileConfig =
                new ProfilesNotificationProperties.ProfileConfig();
        ProfilesNotificationProperties.ProfileConfigVariables customVariables =
                new ProfilesNotificationProperties.ProfileConfigVariables();
        customVariables.setAssistedLink("https://custom.link");
        customProfileConfig.setVariables(customVariables);

        Map<NotificationEvent, NotificationContent> customEvents = new EnumMap<>(NotificationEvent.class);
        customEvents.put(NotificationEvent.SUSPENSION, new NotificationContent("Custom Suspension", "Markdown Suspension"));
        customProfileConfig.setEvents(customEvents);

        Map<String, ProfilesNotificationProperties.ProfileConfig> profileMap = new HashMap<>();
        profileMap.put(ProfilesNotificationCatalog.DEFAULT_PROFILE, defaultProfileConfig);
        profileMap.put("CUSTOM", customProfileConfig);

        properties.setProfiles(profileMap);
        catalog = new ProfilesNotificationCatalog(properties);
    }

    @Test
    void constructor_throwsExceptionWhenDefaultProfileMissing() {
        ProfilesNotificationProperties emptyProperties = new ProfilesNotificationProperties();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new ProfilesNotificationCatalog(emptyProperties)
        );

        assertEquals("The DEFAULT notification profile must be declared", exception.getMessage());
    }

    @Test
    void containsProfile_returnsExpectedResults() {
        assertTrue(catalog.containsProfile("DEFAULT"));
        assertTrue(catalog.containsProfile("CUSTOM"));
        assertFalse(catalog.containsProfile("UNKNOWN"));
        assertFalse(catalog.containsProfile(null));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    void resolveProfile_resolvesToDefaultWhenConfiguredProfileIsBlank(String blankProfile) {
        Optional<String> resolved = catalog.resolveProfile(blankProfile);

        assertTrue(resolved.isPresent());
        assertEquals(ProfilesNotificationCatalog.DEFAULT_PROFILE, resolved.get());
    }

    @Test
    void resolveProfile_returnsConfiguredProfileWhenFound() {
        Optional<String> resolved = catalog.resolveProfile("CUSTOM");

        assertTrue(resolved.isPresent());
        assertEquals("CUSTOM", resolved.get());
    }

    @Test
    void resolveProfile_returnsEmptyWhenProfileNotFound() {
        Optional<String> resolved = catalog.resolveProfile("NON_EXISTING");

        assertFalse(resolved.isPresent());
    }

    @Test
    void find_returnsContentWhenProfileAndEventExist() {
        Optional<NotificationContent> content = catalog.find("DEFAULT", NotificationEvent.ONBOARDING_OK);

        assertTrue(content.isPresent());
        assertEquals("Default OK", content.get().subject());
        assertEquals("Markdown OK", content.get().markdown());
    }

    @Test
    void find_returnsEmptyWhenEventNotInProfile() {
        Optional<NotificationContent> content = catalog.find("DEFAULT", NotificationEvent.SUSPENSION);

        assertFalse(content.isPresent());
    }

    @Test
    void find_returnsEmptyWhenProfileDoesNotExist() {
        Optional<NotificationContent> content = catalog.find("UNKNOWN", NotificationEvent.ONBOARDING_OK);

        assertFalse(content.isPresent());
    }

    @Test
    void find_returnsEmptyWhenProfileIsNull() {
        Optional<NotificationContent> content = catalog.find(null, NotificationEvent.ONBOARDING_OK);

        assertFalse(content.isPresent());
    }

    @Test
    void find_returnsEmptyWhenEventsMapIsNull() {
        ProfilesNotificationProperties.ProfileConfig profileWithNullEvents =
                new ProfilesNotificationProperties.ProfileConfig();
        profileWithNullEvents.setEvents(null);

        properties.getProfiles().put("NULL_EVENTS", profileWithNullEvents);

        Optional<NotificationContent> content = catalog.find("NULL_EVENTS", NotificationEvent.ONBOARDING_OK);

        assertFalse(content.isPresent());
    }

    @Test
    void retrieveProfileVariables_returnsVariablesForConfiguredProfile() {
        Optional<ProfilesNotificationProperties.ProfileConfigVariables> variables =
                catalog.retrieveProfileVariables("CUSTOM");

        assertTrue(variables.isPresent());
        assertEquals("https://custom.link", variables.get().getAssistedLink());
    }

    @Test
    void retrieveProfileVariables_returnsDefaultVariablesWhenBlank() {
        Optional<ProfilesNotificationProperties.ProfileConfigVariables> variables =
                catalog.retrieveProfileVariables(null);

        assertTrue(variables.isPresent());
        assertEquals("https://default.link", variables.get().getAssistedLink());
    }

    @Test
    void retrieveProfileVariables_returnsEmptyWhenProfileNotFound() {
        Optional<ProfilesNotificationProperties.ProfileConfigVariables> variables =
                catalog.retrieveProfileVariables("UNKNOWN");

        assertFalse(variables.isPresent());
    }

    @Test
    void retrieveProfileVariables_returnsEmptyWhenVariablesAreNull() {
        ProfilesNotificationProperties.ProfileConfig profileWithNullVariables =
                new ProfilesNotificationProperties.ProfileConfig();
        profileWithNullVariables.setVariables(null);

        properties.getProfiles().put("NULL_VARS", profileWithNullVariables);

        Optional<ProfilesNotificationProperties.ProfileConfigVariables> variables =
                catalog.retrieveProfileVariables("NULL_VARS");

        assertFalse(variables.isPresent());
    }
}

