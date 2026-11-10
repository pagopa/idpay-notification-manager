package it.gov.pagopa.notification.manager.model;

import org.springframework.util.StringUtils;

public record NotificationContent(String subject, String markdown) {

    public NotificationContent {
        if (!StringUtils.hasText(subject) || !StringUtils.hasText(markdown)) {
            throw new IllegalArgumentException("Notification subject and markdown must not be blank");
        }
    }
}
