package it.gov.pagopa.notification.manager.service.io.placeholder;

import it.gov.pagopa.notification.manager.model.NotificationContent;
import it.gov.pagopa.notification.manager.constants.NotificationConstants;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class NotificationContentRenderer {

    private static final Pattern PLACEHOLDER = Pattern.compile("%([^%]+)%");

    public <T> NotificationContent render(
            NotificationContent content,
            T source,
            Iterable<? extends NotificationPlaceholder<T>> placeholders
    ) {
        Map<String, NotificationPlaceholder<T>> supported = new HashMap<>();
        for (NotificationPlaceholder<T> placeholder : placeholders) {
            if (supported.putIfAbsent(placeholder.key(), placeholder) != null) {
                throw new IllegalArgumentException("Duplicate notification placeholder: " + placeholder.key());
            }
        }
        Map<String, String> values = new HashMap<>();
        return new NotificationContent(
                renderText(content.subject(), source, supported, values),
                renderText(content.markdown(), source, supported, values)
        );
    }

    private <T> String renderText(
            String text,
            T source,
            Map<String, NotificationPlaceholder<T>> supported,
            Map<String, String> values
    ) {
        Matcher matcher = PLACEHOLDER.matcher(text);
        StringBuilder rendered = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = values.computeIfAbsent(key, name -> resolve(name, source, supported));
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(rendered);
        return rendered.toString();
    }

    private <T> String resolve(
            String key,
            T source,
            Map<String, NotificationPlaceholder<T>> supported
    ) {
        NotificationPlaceholder<T> placeholder = supported.get(key);
        if (placeholder == null) {
            throw new IllegalArgumentException("Unsupported notification placeholder: " + key);
        }
        String value = placeholder.resolve(source);
        return StringUtils.hasLength(value) ? value : NotificationConstants.MARKDOWN_NA;
    }
}
