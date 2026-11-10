package it.gov.pagopa.notification.manager.service.io.placeholder;

import it.gov.pagopa.notification.manager.model.Notification;

public record InitiativePlaceholderContext(String initiativeId, String initiativeName) {

    public static InitiativePlaceholderContext from(Notification notification) {
        return new InitiativePlaceholderContext(
                notification.getInitiativeId(),
                notification.getInitiativeName()
        );
    }
}
