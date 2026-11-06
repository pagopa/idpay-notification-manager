package it.gov.pagopa.notification.manager.service.io.placeholder;

import it.gov.pagopa.notification.manager.dto.event.NotificationReminderQueueDTO;
import it.gov.pagopa.notification.manager.model.NotificationContent;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReminderPlaceholderTest {

    private final NotificationContentRenderer renderer = new NotificationContentRenderer();

    @Test
    void from_mapsReminderFields() {
        LocalDate endDate = LocalDate.of(2026, 11, 6);
        NotificationReminderQueueDTO notification = NotificationReminderQueueDTO.builder()
                .initiativeId("initiative-id")
                .initiativeName("Bonus")
                .voucherEndDate(endDate)
                .build();

        assertEquals(
                new ReminderPlaceholderContext("initiative-id", "Bonus", endDate),
                ReminderPlaceholderContext.from(notification)
        );
    }

    @Test
    void render_formatsDateInItalian() {
        NotificationContent rendered = renderer.render(
                new NotificationContent("%initiativeName%", "%initiativeId% scade il %voucherEndDate%"),
                new ReminderPlaceholderContext("initiative-id", "Bonus", LocalDate.of(2026, 11, 6)),
                List.of(ReminderPlaceholder.values())
        );

        assertEquals(
                new NotificationContent("Bonus", "initiative-id scade il 06 novembre 2026"),
                rendered
        );
    }

    @Test
    void render_replacesMissingDateWithNotAvailable() {
        NotificationContent rendered = renderer.render(
                new NotificationContent("Subject", "Scade il %voucherEndDate%"),
                ReminderPlaceholderContext.from(new NotificationReminderQueueDTO()),
                List.of(ReminderPlaceholder.values())
        );

        assertEquals("Scade il N.A.", rendered.markdown());
    }
}
