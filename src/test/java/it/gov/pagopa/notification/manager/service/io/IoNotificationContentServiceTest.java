package it.gov.pagopa.notification.manager.service.io;

import it.gov.pagopa.notification.manager.enums.NotificationEvent;
import it.gov.pagopa.notification.manager.model.NotificationContent;
import it.gov.pagopa.notification.manager.service.io.placeholder.NotificationContentRenderer;
import it.gov.pagopa.notification.manager.service.io.placeholder.NotificationPlaceholder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
class IoNotificationContentServiceTest {

    private final NotificationContentRenderer renderer = new NotificationContentRenderer();

    @Mock
    private ProfilesNotificationCatalog catalog;

    private IoNotificationContentService contentService;

    @BeforeEach
    void setUp() {
        contentService = new IoNotificationContentService(catalog, renderer);
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
        when(catalog.resolveProfile("BEL25")).thenReturn(Optional.of("BEL25"));
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
        when(catalog.resolveProfile("UNKNOWN")).thenReturn(Optional.empty());

        Optional<NotificationContent> result = contentService.resolveProfileAndFindAndRender(
                "UNKNOWN",
                NotificationEvent.ONBOARDING_OK,
                "Ada",
                List.of(namePlaceholder())
        );

        assertEquals(Optional.empty(), result);
        verify(catalog, never()).find(anyString(), any());
    }

    @Test
    void resolveProfileAndFind_returnsContentWithoutRendering() {
        when(catalog.resolveProfile("BEL25")).thenReturn(Optional.of("BEL25"));
        when(catalog.find("BEL25", NotificationEvent.ONBOARDING_OK))
                .thenReturn(Optional.of(new NotificationContent("Hello %name%", "Welcome %name%")));

        assertEquals(
                Optional.of(new NotificationContent("Hello %name%", "Welcome %name%")),
                contentService.resolveProfileAndFind("BEL25", NotificationEvent.ONBOARDING_OK)
        );
    }

    @Test
    void resolveProfileAndFind_skipsUnknownProfile() {
        when(catalog.resolveProfile("UNKNOWN")).thenReturn(Optional.empty());

        assertEquals(
                Optional.empty(),
                contentService.resolveProfileAndFind("UNKNOWN", NotificationEvent.ONBOARDING_OK)
        );
        verify(catalog, never()).find(anyString(), any());
    }

    @Test
    void resolveProfileAndFindAndRender_usesProfileReturnedByCatalog() {
        when(catalog.resolveProfile(null)).thenReturn(Optional.of("DEFAULT"));
        when(catalog.find("DEFAULT", NotificationEvent.ONBOARDING_OK))
                .thenReturn(Optional.of(new NotificationContent("Hello %name%", "Welcome %name%")));

        assertEquals(
                Optional.of(new NotificationContent("Hello Ada", "Welcome Ada")),
                contentService.resolveProfileAndFindAndRender(
                        null, NotificationEvent.ONBOARDING_OK, "Ada", List.of(namePlaceholder()))
        );
    }

    @Test
    void resolveProfileAndFindAndRender_skipsMissingEvent() {
        when(catalog.resolveProfile("BEL25")).thenReturn(Optional.of("BEL25"));
        when(catalog.find("BEL25", NotificationEvent.ONBOARDING_OK)).thenReturn(Optional.empty());

        assertEquals(
                Optional.empty(),
                contentService.resolveProfileAndFindAndRender(
                        "BEL25", NotificationEvent.ONBOARDING_OK, "Ada", List.of(namePlaceholder()))
        );
        verify(catalog, never()).find("DEFAULT", NotificationEvent.ONBOARDING_OK);
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
