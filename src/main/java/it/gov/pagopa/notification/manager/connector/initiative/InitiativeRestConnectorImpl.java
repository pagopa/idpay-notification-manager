package it.gov.pagopa.notification.manager.connector.initiative;

import it.gov.pagopa.notification.manager.dto.initiative.InitiativeAdditionalInfoDTO;
import it.gov.pagopa.notification.manager.dto.initiative.InitiativeNotificationDTO;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class InitiativeRestConnectorImpl implements InitiativeRestConnector {

    private final InitiativeFeignRestClient initiativeFeignRestClient;

    public InitiativeRestConnectorImpl(
            InitiativeFeignRestClient initiativeFeignRestClient) {
        this.initiativeFeignRestClient = initiativeFeignRestClient;
    }

    @Override
    @Cacheable(value = "initiativeToken", key = "#initiativeId")
    public InitiativeAdditionalInfoDTO getIOTokens(String initiativeId) {
        return initiativeFeignRestClient.getTokens(initiativeId).getBody();
    }

    @Override
    @Cacheable(value = "initiativeEmailFlux", key = "#initiativeId")
    public InitiativeNotificationDTO getInitiativeDetailInfo(String initiativeId) {
        return initiativeFeignRestClient.getInitiativeDetailInfo(initiativeId).getBody();
    }

}
