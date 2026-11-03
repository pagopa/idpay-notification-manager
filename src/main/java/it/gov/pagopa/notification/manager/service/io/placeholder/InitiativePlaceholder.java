package it.gov.pagopa.notification.manager.service.io.placeholder;

import it.gov.pagopa.notification.manager.model.Notification;

import java.util.function.Function;

public enum InitiativePlaceholder implements NotificationPlaceholder<Notification> {

    INITIATIVE_ID("initiativeId", Notification::getInitiativeId),
    INITIATIVE_NAME("initiativeName", Notification::getInitiativeName);

    private final String key;
    private final Function<Notification, String> resolver;

    InitiativePlaceholder(String key, Function<Notification, String> resolver) {
        this.key = key;
        this.resolver = resolver;
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public String resolve(Notification source) {
        return resolver.apply(source);
    }
}
