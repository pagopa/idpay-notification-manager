package it.gov.pagopa.notification.manager.service.profile;

public sealed interface NotificationProfileResolution {

    record Resolved(String profile) implements NotificationProfileResolution {}

    record Skipped(String configuredProfile) implements NotificationProfileResolution {}

    record Failed(FailureReason reason) implements NotificationProfileResolution {}

    enum FailureReason {
        INITIATIVE_FETCH_FAILED,
        EMPTY_INITIATIVE_RESPONSE
    }
}
