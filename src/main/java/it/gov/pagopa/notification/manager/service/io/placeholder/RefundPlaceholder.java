package it.gov.pagopa.notification.manager.service.io.placeholder;

import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.notification.manager.dto.event.NotificationRefundQueueDTO;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

public enum RefundPlaceholder implements NotificationPlaceholder<NotificationRefundQueueDTO> {

    EFFECTIVE_REWARD;

    @Override
    public String key() {
        return "effectiveReward";
    }

    @Override
    public String resolve(NotificationRefundQueueDTO source) {
        if (source.getRefundReward() == null) {
            return null;
        }
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setDecimalSeparator(',');
        DecimalFormat format = new DecimalFormat("###.00", symbols);
        format.setGroupingUsed(false);
        return format.format(CommonUtilities.centsToEuro(source.getRefundReward()));
    }
}
