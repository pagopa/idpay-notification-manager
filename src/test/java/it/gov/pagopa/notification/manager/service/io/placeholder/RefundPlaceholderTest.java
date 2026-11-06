package it.gov.pagopa.notification.manager.service.io.placeholder;

import it.gov.pagopa.notification.manager.model.NotificationContent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RefundPlaceholderTest {

    private final NotificationContentRenderer renderer = new NotificationContentRenderer();

    @Test
    void from_preservesBigDecimalAmountWithoutTruncation() {
        BigDecimal reward = new BigDecimal("123.456");

        assertEquals(new RefundPlaceholderContext(reward), RefundPlaceholderContext.from(reward));
    }

    @Test
    void render_formatsRefundRewardInEuros() {
        NotificationContent rendered = renderer.render(
                new NotificationContent("Rimborso", "Hai ricevuto %effectiveReward% euro"),
                new RefundPlaceholderContext(new BigDecimal("123.45")),
                List.of(RefundPlaceholder.values())
        );

        assertEquals("Hai ricevuto 123,45 euro", rendered.markdown());
    }

    @Test
    void render_replacesMissingRewardWithNotAvailable() {
        NotificationContent rendered = renderer.render(
                new NotificationContent("Rimborso", "Hai ricevuto %effectiveReward% euro"),
                RefundPlaceholderContext.from(null),
                List.of(RefundPlaceholder.values())
        );

        assertEquals("Hai ricevuto N.A. euro", rendered.markdown());
    }
}
