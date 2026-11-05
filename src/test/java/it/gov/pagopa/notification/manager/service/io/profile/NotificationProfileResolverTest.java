package it.gov.pagopa.notification.manager.service.io.profile;

import it.gov.pagopa.notification.manager.connector.initiative.InitiativeRestConnector;
import it.gov.pagopa.notification.manager.service.io.IoNotificationCatalog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationProfileResolverTest {

    @Mock
    private InitiativeRestConnector initiativeRestConnector;
    @Mock
    private IoNotificationCatalog catalog;
    @InjectMocks
    private NotificationProfileResolver resolver;

    @Test
    void resolveConfiguredProfile_returnsResolvedProfileWhenConfigured() {
        when(catalog.containsProfile("BEL25")).thenReturn(true);

        assertEquals(
                new NotificationProfileResolution.Resolved("BEL25"),
                resolver.resolveByInputProfile("BEL25")
        );
        verify(catalog).containsProfile("BEL25");
    }

    @Test
    void resolveConfiguredProfile_usesDefaultWhenProfileIsBlank() {
        when(catalog.containsProfile(IoNotificationCatalog.DEFAULT_PROFILE)).thenReturn(true);

        assertEquals(
                new NotificationProfileResolution.Resolved(IoNotificationCatalog.DEFAULT_PROFILE),
                resolver.resolveByInputProfile(" ")
        );
    }

    @Test
    void resolveConfiguredProfile_skipsUnknownProfile() {
        when(catalog.containsProfile("UNKNOWN")).thenReturn(false);

        assertEquals(
                new NotificationProfileResolution.Skipped("UNKNOWN"),
                resolver.resolveByInputProfile("UNKNOWN")
        );
    }
}
