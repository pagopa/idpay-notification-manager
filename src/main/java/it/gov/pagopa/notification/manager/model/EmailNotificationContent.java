package it.gov.pagopa.notification.manager.model;

import org.springframework.util.StringUtils;

public record EmailNotificationContent(String subject, String templateName) {

    public EmailNotificationContent {
        if (!StringUtils.hasText(subject) || !StringUtils.hasText(templateName)) {
            throw new IllegalArgumentException("Notification subject and templateName must not be blank");
        }
    }
}
