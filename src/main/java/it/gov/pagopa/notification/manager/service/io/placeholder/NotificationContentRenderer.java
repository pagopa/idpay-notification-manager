package it.gov.pagopa.notification.manager.service.io.placeholder;

import it.gov.pagopa.notification.manager.model.NotificationContent;
import it.gov.pagopa.notification.manager.constants.NotificationConstants;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class NotificationContentRenderer {

    public <T> NotificationContent render(
            NotificationContent content,
            T source,
            Iterable<? extends NotificationPlaceholder<T>> placeholders
    ) {
        String subject = content.subject();
        String markdown = content.markdown();
        for (NotificationPlaceholder<T> placeholder : placeholders) {
            String tag = NotificationConstants.MARKDOWN_TAG
                    + placeholder.key() + NotificationConstants.MARKDOWN_TAG;
            if (!subject.contains(tag) && !markdown.contains(tag)) {
                continue;
            }
            String value = placeholder.resolve(source);
            String replacement = StringUtils.hasLength(value) ? value : NotificationConstants.MARKDOWN_NA;
            subject = subject.replace(tag, replacement);
            markdown = markdown.replace(tag, replacement);
        }
        return new NotificationContent(subject, markdown);
    }
}
