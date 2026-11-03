package it.gov.pagopa.notification.manager.service.io;

import it.gov.pagopa.notification.manager.enums.NotificationEvent;
import it.gov.pagopa.notification.manager.model.NotificationContent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static it.gov.pagopa.notification.manager.service.onboarding.BaseOnboardingNotification.sanitizeString;

@Service
@RequiredArgsConstructor
@Slf4j
public class IoNotificationContentService {

    private final IoNotificationCatalog catalog;

    public Optional<NotificationContent> find(String profile, NotificationEvent event) {
        Optional<NotificationContent> content = catalog.find(profile, event);
        if (content.isEmpty()) {
            log.warn("[NOTIFY][IO][NOTIFICATION_CONTENT_NOT_CONFIGURED] profile={}, event={}",
                    sanitizeString(profile), event);
        }
        return content;
    }
}
