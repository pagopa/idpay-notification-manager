package it.gov.pagopa.notification.manager.service.io.placeholder;

import java.util.function.Function;

public enum InitiativePlaceholder implements NotificationPlaceholder<InitiativePlaceholderContext> {

    INITIATIVE_ID("initiativeId", InitiativePlaceholderContext::initiativeId),
    INITIATIVE_NAME("initiativeName", InitiativePlaceholderContext::initiativeName);

    private final String key;
    private final Function<InitiativePlaceholderContext, String> resolver;

    InitiativePlaceholder(String key, Function<InitiativePlaceholderContext, String> resolver) {
        this.key = key;
        this.resolver = resolver;
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public String resolve(InitiativePlaceholderContext source) {
        return resolver.apply(source);
    }
}
