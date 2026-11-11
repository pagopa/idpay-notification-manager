package it.gov.pagopa.notification.manager.service.io.placeholder;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.function.Function;

public enum RefundPlaceholder implements NotificationPlaceholder<RefundPlaceholderContext> {

    EFFECTIVE_REWARD("effectiveReward", RefundPlaceholder::effectiveReward);

    private final String key;
    private final Function<RefundPlaceholderContext, String> resolver;

    RefundPlaceholder(String key, Function<RefundPlaceholderContext, String> resolver) {
        this.key = key;
        this.resolver = resolver;
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public String resolve(RefundPlaceholderContext source) {
        return resolver.apply(source);
    }

    private static String effectiveReward(RefundPlaceholderContext source) {
        if (source.refundReward() == null) {
            return null;
        }
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setDecimalSeparator(',');
        DecimalFormat format = new DecimalFormat("###.00", symbols);
        format.setGroupingUsed(false);
        return format.format(source.refundReward());
    }
}
