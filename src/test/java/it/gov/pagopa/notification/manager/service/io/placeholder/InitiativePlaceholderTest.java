package it.gov.pagopa.notification.manager.service.io.placeholder;

import it.gov.pagopa.notification.manager.model.NotificationContent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InitiativePlaceholderTest {

    private final NotificationContentRenderer renderer = new NotificationContentRenderer();

    @Test
    void render_usesInitiativeContextFields() {
        NotificationContent rendered = renderer.render(
                new NotificationContent("Bonus %initiativeName%", "Apri %initiativeId%"),
                new InitiativePlaceholderContext("initiative-id", "Test"),
                List.of(InitiativePlaceholder.values())
        );

        assertEquals(new NotificationContent("Bonus Test", "Apri initiative-id"), rendered);
    }

    @Test
    void render_replacesMissingContextValuesWithNotAvailable() {
        NotificationContent rendered = renderer.render(
                new NotificationContent("Bonus %initiativeName%", "Apri %initiativeId%"),
                new InitiativePlaceholderContext(null, ""),
                List.of(InitiativePlaceholder.values())
        );

        assertEquals(new NotificationContent("Bonus N.A.", "Apri N.A."), rendered);
    }
}
