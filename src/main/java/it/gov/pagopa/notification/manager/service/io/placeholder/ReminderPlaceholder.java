package it.gov.pagopa.notification.manager.service.io.placeholder;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Function;

public enum ReminderPlaceholder implements NotificationPlaceholder<ReminderPlaceholderContext> {

    INITIATIVE_ID("initiativeId", ReminderPlaceholderContext::initiativeId),
    INITIATIVE_NAME("initiativeName", ReminderPlaceholderContext::initiativeName),
    VOUCHER_END_DATE("voucherEndDate", ReminderPlaceholder::voucherEndDate);

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd MMMM yyyy").withLocale(Locale.ITALIAN);

    private final String key;
    private final Function<ReminderPlaceholderContext, String> resolver;

    ReminderPlaceholder(String key, Function<ReminderPlaceholderContext, String> resolver) {
        this.key = key;
        this.resolver = resolver;
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public String resolve(ReminderPlaceholderContext source) {
        return resolver.apply(source);
    }

    private static String voucherEndDate(ReminderPlaceholderContext source) {
        return source.voucherEndDate() == null ? null : source.voucherEndDate().format(DATE_FORMAT);
    }
}
