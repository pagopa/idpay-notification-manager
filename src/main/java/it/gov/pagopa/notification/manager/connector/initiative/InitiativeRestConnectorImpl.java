package it.gov.pagopa.notification.manager.connector.initiative;

import it.gov.pagopa.notification.manager.dto.initiative.InitiativeAdditionalInfoDTO;
import org.springframework.stereotype.Service;

@Service
public class InitiativeRestConnectorImpl implements InitiativeRestConnector {

    private final InitiativeFeignRestClient initiativeFeignRestClient;

    public InitiativeRestConnectorImpl(
            InitiativeFeignRestClient initiativeFeignRestClient) {
        this.initiativeFeignRestClient = initiativeFeignRestClient;
    }

    @Override
    public InitiativeAdditionalInfoDTO getIOTokens(String initiativeId) {
        return initiativeFeignRestClient.getTokens(initiativeId).getBody();
    }

}
