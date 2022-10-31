package it.gov.pagopa.notification.manager.dto.event;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import static it.gov.pagopa.notification.manager.constants.NotificationConstants.AnyNotificationConsumer.SubTypes.ALLOWED_CITIZEN_PUBLISH;
import static it.gov.pagopa.notification.manager.constants.NotificationConstants.AnyNotificationConsumer.SubTypes.CHECKIBAN_KO;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "operationType",
        visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = NotificationCitizenOnQueueDTO.class, name = ALLOWED_CITIZEN_PUBLISH),
        @JsonSubTypes.Type(value = NotificationIbanQueueDTO.class, name = CHECKIBAN_KO)
})
public interface AnyOfNotificationQueueDTO {
}
