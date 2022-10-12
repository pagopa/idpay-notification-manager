package it.gov.pagopa.notification.manager.connector.initiative;

import it.gov.pagopa.notification.manager.dto.initiative.InitiativeAdditionalInfoDTO;

public interface InitiativeRestConnector {

    InitiativeAdditionalInfoDTO getIOTokens(String initiativeId);

}
