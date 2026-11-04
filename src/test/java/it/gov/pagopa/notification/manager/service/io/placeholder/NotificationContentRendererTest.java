package it.gov.pagopa.notification.manager.service.io.placeholder;

import it.gov.pagopa.notification.manager.model.NotificationContent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificationContentRendererTest {

    private final NotificationContentRenderer renderer = new NotificationContentRenderer();

    @Test
    void render_replacesEmptyPlaceholderValueWithNotAvailableMarker() {
        NotificationContent rendered = renderer.render(
                new NotificationContent("Subject", "before[%value%]after"),
                "source",
                List.of(placeholder(""))
        );

        assertEquals("before[N.A.]after", rendered.markdown());
    }

    @Test
    void render_preservesWhitespacePlaceholderValue() {
        NotificationContent rendered = renderer.render(
                new NotificationContent("Subject", "before[%value%]after"),
                "source",
                List.of(placeholder("  \n "))
        );

        assertEquals("before[  \n ]after", rendered.markdown());
    }

    @Test
    void render_leavesSpringStylePlaceholdersUnchanged() {
        NotificationContent rendered = renderer.render(
                new NotificationContent("Subject", "${property} %value%"),
                "source",
                List.of(placeholder("resolved"))
        );

        assertEquals("${property} resolved", rendered.markdown());
    }

    @Test
    void render_replacesNullPlaceholderValueWithNotAvailableMarker() {
        NotificationContent rendered = renderer.render(
                new NotificationContent("Subject", "before[%value%]after"),
                "source",
                List.of(placeholder(null))
        );

        assertEquals("before[N.A.]after", rendered.markdown());
    }

    private NotificationPlaceholder<String> placeholder(String value) {
        return new NotificationPlaceholder<>() {
            @Override
            public String key() {
                return "value";
            }

            @Override
            public String resolve(String source) {
                return value;
            }
        };
    }
}
