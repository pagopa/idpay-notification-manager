package it.gov.pagopa.notification.manager.service.io.placeholder;

import java.math.BigDecimal;

public record RefundPlaceholderContext(BigDecimal refundReward) {

    public static RefundPlaceholderContext from(BigDecimal refundReward) {
        return new RefundPlaceholderContext(refundReward);
    }
}
