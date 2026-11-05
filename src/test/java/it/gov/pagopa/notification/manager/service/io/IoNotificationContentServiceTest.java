package it.gov.pagopa.notification.manager.service.io;

import it.gov.pagopa.notification.manager.enums.NotificationEvent;
import it.gov.pagopa.notification.manager.model.NotificationContent;
import it.gov.pagopa.notification.manager.service.io.placeholder.NotificationContentRenderer;
import it.gov.pagopa.notification.manager.service.io.placeholder.NotificationPlaceholder;
import it.gov.pagopa.notification.manager.service.io.profile.NotificationProfileResolution;
import it.gov.pagopa.notification.manager.service.io.profile.NotificationProfileResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IoNotificationContentServiceTest {

    private final NotificationContentRenderer renderer = new NotificationContentRenderer();

    @Mock
    private IoNotificationCatalog catalog;
    @Mock
    private NotificationProfileResolver profileResolver;

    private IoNotificationContentService contentService;

    @BeforeEach
    void setUp() {
        contentService = new IoNotificationContentService(catalog, renderer, profileResolver);
    }

    @Test
    void findAndRender_loadsContentAndSubstitutesPlaceholderValues() {
        when(catalog.find("BEL25", NotificationEvent.ONBOARDING_OK))
                .thenReturn(Optional.of(new NotificationContent("Hello %name%", "Welcome %name%")));

        Optional<NotificationContent> result = contentService.findAndRender(
                "BEL25",
                NotificationEvent.ONBOARDING_OK,
                "Ada",
                List.of(namePlaceholder())
        );

        assertEquals(Optional.of(new NotificationContent("Hello Ada", "Welcome Ada")), result);
    }

    @Test
    void resolveProfileAndFindAndRender_resolvesProfileBeforeLoadingContent() {
        when(profileResolver.resolveByInputProfile("BEL25"))
                .thenReturn(new NotificationProfileResolution.Resolved("BEL25"));
        when(catalog.find("BEL25", NotificationEvent.ONBOARDING_OK))
                .thenReturn(Optional.of(new NotificationContent("Hello %name%", "Welcome %name%")));

        Optional<NotificationContent> result = contentService.resolveProfileAndFindAndRender(
                "BEL25",
                NotificationEvent.ONBOARDING_OK,
                "Ada",
                List.of(namePlaceholder())
        );

        assertEquals(Optional.of(new NotificationContent("Hello Ada", "Welcome Ada")), result);
    }

    @Test
    void resolveProfileAndFindAndRender_skipsContentLookupForUnknownProfile() {
        when(profileResolver.resolveByInputProfile("UNKNOWN"))
                .thenReturn(new NotificationProfileResolution.Skipped("UNKNOWN"));

        Optional<NotificationContent> result = contentService.resolveProfileAndFindAndRender(
                "UNKNOWN",
                NotificationEvent.ONBOARDING_OK,
                "Ada",
                List.of(namePlaceholder())
        );

        assertEquals(Optional.empty(), result);
    }

    private NotificationPlaceholder<String> namePlaceholder() {
        return new NotificationPlaceholder<>() {
            @Override
            public String key() {
                return "name";
            }

            @Override
            public String resolve(String source) {
                return source;
            }
        };
    }
}
