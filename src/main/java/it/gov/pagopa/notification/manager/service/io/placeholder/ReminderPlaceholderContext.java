package it.gov.pagopa.notification.manager.service.io.placeholder;

import it.gov.pagopa.notification.manager.dto.event.NotificationReminderQueueDTO;

import java.time.LocalDate;

public record ReminderPlaceholderContext(
        String initiativeId,
        String initiativeName,
        LocalDate voucherEndDate
) {
    public static ReminderPlaceholderContext from(NotificationReminderQueueDTO notification) {
        return new ReminderPlaceholderContext(
                notification.getInitiativeId(),
                notification.getInitiativeName(),
                notification.getVoucherEndDate()
        );
    }
}
