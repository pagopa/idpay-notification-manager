package it.gov.pagopa.notification.manager.service.io.profile;

import feign.FeignException;
import it.gov.pagopa.notification.manager.connector.initiative.InitiativeRestConnector;
import it.gov.pagopa.notification.manager.dto.initiative.InitiativeNotificationDTO;
import it.gov.pagopa.notification.manager.service.io.IoNotificationCatalog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import static it.gov.pagopa.notification.manager.service.onboarding.BaseOnboardingNotification.sanitizeString;
import static it.gov.pagopa.notification.manager.service.io.profile.NotificationProfileResolution.FailureReason.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationProfileResolver {

    private final InitiativeRestConnector initiativeRestConnector;
    private final IoNotificationCatalog catalog;

    public NotificationProfileResolution resolveByInitiativeId(String initiativeId) {
        if (!StringUtils.hasText(initiativeId)) {
            throw new IllegalArgumentException("An initiativeId is required to resolve the IO profile");
        }
        String sanitizedInitiativeId = sanitizeString(initiativeId);
        InitiativeNotificationDTO initiative;
        try {
            initiative = initiativeRestConnector.getInitiativeDetailInfo(initiativeId);
        } catch (FeignException exception) {
            log.error("[NOTIFY][IO][INITIATIVE_FETCH_FAILED] initiativeId={}, status={}",
                    sanitizedInitiativeId, exception.status());
            return new NotificationProfileResolution.Failed(INITIATIVE_FETCH_FAILED);
        }
        if (initiative == null) {
            log.error("[NOTIFY][IO][EMPTY_INITIATIVE_RESPONSE] initiativeId={}",
                    sanitizedInitiativeId);
            return new NotificationProfileResolution.Failed(EMPTY_INITIATIVE_RESPONSE);
        }
        return resolveByInputProfile(initiative.getEmailFlux(), sanitizedInitiativeId);
    }

    public NotificationProfileResolution resolveByInputProfile(String configuredProfile) {
        return resolveByInputProfile(configuredProfile, null);
    }

    private NotificationProfileResolution resolveByInputProfile(String configuredProfile, String sanitizedInitiativeId) {
        String profile = StringUtils.hasText(configuredProfile)
                ? configuredProfile
                : IoNotificationCatalog.DEFAULT_PROFILE;
        if (!catalog.containsProfile(profile)) {
            if (sanitizedInitiativeId != null) {
                log.warn("[NOTIFY][IO][UNKNOWN_NOTIFICATION_PROFILE] initiativeId={}, profile={}",
                        sanitizedInitiativeId, sanitizeString(profile));
            } else {
                log.warn("[NOTIFY][IO][UNKNOWN_NOTIFICATION_PROFILE] profile={}", sanitizeString(profile));
            }
            return new NotificationProfileResolution.Skipped(profile);
        }
        if (sanitizedInitiativeId != null) {
            log.info("[NOTIFY][IO][PROFILE_RESOLVED] initiativeId={}, profile={}",
                    sanitizedInitiativeId, profile);
        } else {
            log.info("[NOTIFY][IO][PROFILE_RESOLVED] profile={}", profile);
        }
        return new NotificationProfileResolution.Resolved(profile);
    }
}
