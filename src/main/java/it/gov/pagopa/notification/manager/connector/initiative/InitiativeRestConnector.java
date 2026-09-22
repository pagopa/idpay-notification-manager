package it.gov.pagopa.notification.manager.connector.initiative;

import it.gov.pagopa.notification.manager.dto.initiative.InitiativeAdditionalInfoDTO;
import it.gov.pagopa.notification.manager.dto.initiative.InitiativeNotificationDTO;
import org.springframework.cache.annotation.Cacheable;

public interface InitiativeRestConnector {

    InitiativeAdditionalInfoDTO getIOTokens(String initiativeId);

    InitiativeNotificationDTO getInitiativeDetailInfo(String initiativeId);

}
