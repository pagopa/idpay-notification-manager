package it.gov.pagopa.notification.manager.service.io.placeholder;

import it.gov.pagopa.notification.manager.model.NotificationContent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificationContentRendererTest {

    private final NotificationContentRenderer renderer = new NotificationContentRenderer();

    @Test
    void render_replacesEveryOccurrenceInSubjectAndMarkdownLiterally() {
        NotificationContent rendered = renderer.render(
                new NotificationContent("%value% %value%", "before[%value%]after"),
                "source",
                List.of(placeholder("$5\\path"))
        );

        assertEquals("$5\\path $5\\path", rendered.subject());
        assertEquals("before[$5\\path]after", rendered.markdown());
    }

    @Test
    void render_leavesUnknownTagsUnchanged() {
        NotificationContent rendered = renderer.render(
                new NotificationContent("Subject", "%unknown% %value%"),
                "source",
                List.of(placeholder("resolved"))
        );

        assertEquals("%unknown% resolved", rendered.markdown());
    }

    @Test
    void render_doesNotResolveUnusedPlaceholder() {
        NotificationPlaceholder<String> unused = new NotificationPlaceholder<>() {
            @Override
            public String key() {
                return "unused";
            }

            @Override
            public String resolve(String source) {
                throw new AssertionError("Unused placeholder must not be resolved");
            }
        };
        NotificationContent content = new NotificationContent("Subject", "Markdown");

        assertEquals(content, renderer.render(content, "source", List.of(unused)));
    }

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
