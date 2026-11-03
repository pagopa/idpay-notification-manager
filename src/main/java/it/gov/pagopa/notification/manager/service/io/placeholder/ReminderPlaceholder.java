package it.gov.pagopa.notification.manager.service.io.placeholder;

import it.gov.pagopa.notification.manager.dto.event.NotificationReminderQueueDTO;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Function;

public enum ReminderPlaceholder implements NotificationPlaceholder<NotificationReminderQueueDTO> {

    INITIATIVE_ID("initiativeId", NotificationReminderQueueDTO::getInitiativeId),
    INITIATIVE_NAME("initiativeName", NotificationReminderQueueDTO::getInitiativeName),
    VOUCHER_END_DATE("voucherEndDate", ReminderPlaceholder::voucherEndDate);

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ITALIAN);

    private final String key;
    private final Function<NotificationReminderQueueDTO, String> resolver;

    ReminderPlaceholder(String key, Function<NotificationReminderQueueDTO, String> resolver) {
        this.key = key;
        this.resolver = resolver;
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public String resolve(NotificationReminderQueueDTO source) {
        return resolver.apply(source);
    }

    private static String voucherEndDate(NotificationReminderQueueDTO source) {
        return source.getVoucherEndDate() == null ? null : source.getVoucherEndDate().format(DATE_FORMAT);
    }
}
