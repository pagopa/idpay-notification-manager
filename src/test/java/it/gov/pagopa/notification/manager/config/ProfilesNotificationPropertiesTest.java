package it.gov.pagopa.notification.manager.config;

import it.gov.pagopa.notification.manager.enums.NotificationEvent;
import it.gov.pagopa.notification.manager.model.EmailNotificationContent;
import it.gov.pagopa.notification.manager.model.NotificationContent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
        "notification.manager.profiles.default.variables.assisted-link=https://example.com/assistenza",
        "notification.manager.profiles.default.events.ONBOARDING_OK.subject=Richiesta di adesione accettata",
        "notification.manager.profiles.default.events.ONBOARDING_OK.markdown=La tua richiesta è stata approvata",
        "notification.manager.profiles.default.events.ONBOARDING_KO_GENERIC.subject=Richiesta non accolta",
        "notification.manager.profiles.default.events.ONBOARDING_KO_GENERIC.markdown=La tua richiesta non è stata accolta",
        "notification.manager.profiles.default.email-events.ONBOARDING_OK.subject=Email subject",
        "notification.manager.profiles.default.email-events.ONBOARDING_OK.template-name=Email_DEFAULT/EsitoOk"
})
@EnableConfigurationProperties(value = ProfilesNotificationProperties.class)
class ProfilesNotificationPropertiesTest {

    @Autowired
    private ProfilesNotificationProperties properties;

    @Test
    void propertiesShouldBeBoundCorrectly() {
        Assertions.assertNotNull(properties);
        Assertions.assertNotNull(properties.getProfiles());
        Assertions.assertTrue(properties.getProfiles().containsKey("default"));

        ProfilesNotificationProperties.ProfileConfig profileConfig = properties.getProfiles().get("default");
        Assertions.assertNotNull(profileConfig);

        // Verify variables binding
        Assertions.assertNotNull(profileConfig.getVariables());
        Assertions.assertEquals("https://example.com/assistenza", profileConfig.getVariables().getAssistedLink());

        // Verify events binding
        Map<NotificationEvent, NotificationContent> events = profileConfig.getEvents();
        Assertions.assertNotNull(events);
        Assertions.assertEquals(2, events.size());

        NotificationContent okContent = events.get(NotificationEvent.ONBOARDING_OK);
        Assertions.assertNotNull(okContent);
        Assertions.assertEquals("Richiesta di adesione accettata", okContent.subject());
        Assertions.assertEquals("La tua richiesta è stata approvata", okContent.markdown());

        NotificationContent koContent = events.get(NotificationEvent.ONBOARDING_KO_GENERIC);
        Assertions.assertNotNull(koContent);
        Assertions.assertEquals("Richiesta non accolta", koContent.subject());
        Assertions.assertEquals("La tua richiesta non è stata accolta", koContent.markdown());

        Assertions.assertEquals(
                new EmailNotificationContent("Email subject", "Email_DEFAULT/EsitoOk"),
                profileConfig.getEmailEvents().get(NotificationEvent.ONBOARDING_OK));
    }

    @Test
    void testPojoGettersAndSetters() {
        ProfilesNotificationProperties customProps = new ProfilesNotificationProperties();
        Assertions.assertNotNull(customProps.getProfiles());
        Assertions.assertTrue(customProps.getProfiles().isEmpty());

        ProfilesNotificationProperties.ProfileConfig profileConfig = new ProfilesNotificationProperties.ProfileConfig();
        Assertions.assertNotNull(profileConfig.getVariables());
        Assertions.assertNotNull(profileConfig.getEvents());

        ProfilesNotificationProperties.ProfileConfigVariables variables = new ProfilesNotificationProperties.ProfileConfigVariables();
        variables.setAssistedLink("https://custom.link");
        Assertions.assertEquals("https://custom.link", variables.getAssistedLink());

        profileConfig.setVariables(variables);
        Assertions.assertSame(variables, profileConfig.getVariables());

        Map<NotificationEvent, NotificationContent> eventMap = new EnumMap<>(NotificationEvent.class);
        eventMap.put(NotificationEvent.SUSPENSION, new NotificationContent("Sospensione", "Iniziativa sospesa"));
        profileConfig.setEvents(eventMap);
        Assertions.assertEquals(eventMap, profileConfig.getEvents());

        Map<String, ProfilesNotificationProperties.ProfileConfig> profileMap = new HashMap<>();
        profileMap.put("customProfile", profileConfig);
        customProps.setProfiles(profileMap);

        Assertions.assertEquals(1, customProps.getProfiles().size());
        Assertions.assertSame(profileConfig, customProps.getProfiles().get("customProfile"));
    }

    @Test
    void testToString() {
        ProfilesNotificationProperties.ProfileConfig profileConfig = new ProfilesNotificationProperties.ProfileConfig();
        ProfilesNotificationProperties.ProfileConfigVariables variables = new ProfilesNotificationProperties.ProfileConfigVariables();
        variables.setAssistedLink("https://test.link");
        profileConfig.setVariables(variables);

        String variablesToString = variables.toString();
        Assertions.assertNotNull(variablesToString);
        Assertions.assertTrue(variablesToString.contains("assistedLink=https://test.link"));

        String configToString = profileConfig.toString();
        Assertions.assertNotNull(configToString);
        Assertions.assertTrue(configToString.contains("variables="));
        Assertions.assertTrue(configToString.contains("events="));
    }
}
