package it.gov.pagopa.notification.manager.service.onboarding;

import feign.FeignException;
import it.gov.pagopa.notification.manager.connector.initiative.InitiativeRestConnector;
import it.gov.pagopa.notification.manager.dto.EvaluationDTO;
import it.gov.pagopa.notification.manager.dto.initiative.InitiativeNotificationDTO;
import it.gov.pagopa.notification.manager.utils.Utilities;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import static it.gov.pagopa.notification.manager.constants.NotificationConstants.*;
import static it.gov.pagopa.notification.manager.service.io.NotificationEventResolver.isOnboardingOkPartial;


@Slf4j
public abstract class BaseOnboardingNotification<R> {

    protected final InitiativeRestConnector initiativeRestConnector;
    protected static final String ZONE_ID_ROME = "Europe/Rome";

    protected BaseOnboardingNotification(InitiativeRestConnector initiativeRestConnector) {
        this.initiativeRestConnector = initiativeRestConnector;
    }

    public String processNotification(EvaluationDTO evaluationDTO){
        String sanitizedUserId = Utilities.sanitizeString(evaluationDTO.getUserId());

        boolean initiativeFetchFailed = false;
        try {
            InitiativeNotificationDTO initiativeNotificationDTO = initiativeRestConnector.getInitiativeDetailInfo(evaluationDTO.getInitiativeId());
            evaluationDTO.setEmailFlux(initiativeNotificationDTO.getEmailFlux());
        } catch (FeignException _) {
            log.error("[PROCESS_ONBOARDING_NOTIFICATION] Failed to retrieve initiativeDetail from initiative service. Notification will not be sent for user {}", sanitizedUserId);
            handleInitiativeFetchError(evaluationDTO);
            return null;
        }

        R notificationToSend = getNotificationFromEvalStatus(evaluationDTO);

        if(notificationToSend != null){
            return sendNotification(notificationToSend, evaluationDTO, initiativeFetchFailed);
        }

        return null;
    }

    protected @Nullable R getNotificationFromEvalStatus(EvaluationDTO evaluationDTO) {
        return switch (evaluationDTO.getStatus()){
            case STATUS_ONBOARDING_OK -> processOnboardingOk(evaluationDTO);
            case STATUS_ONBOARDING_JOINED -> processOnboardingJoined(evaluationDTO);
            case STATUS_ONBOARDING_KO -> processOnboardingKo(evaluationDTO);
            default -> {log.info("[NOTIFY] Unsupported notification for status {} for user {}",
                    Utilities.sanitizeString(evaluationDTO.getStatus()),
                    Utilities.sanitizeString(evaluationDTO.getUserId()));
                            yield null;
            }
        };
    }

    protected void handleInitiativeFetchError(EvaluationDTO evaluationDTO) {}

    abstract R processOnboardingJoined(EvaluationDTO evaluationDTO);

    abstract R processOnboardingKo(EvaluationDTO evaluationDTO);

    private R processOnboardingOk(EvaluationDTO evaluationDTO) {
        boolean isPartial = isOnboardingOkPartial(evaluationDTO.getVerifies(), evaluationDTO.getBeneficiaryBudgetCents());
        return generateOnboardingOkNotification(isPartial, evaluationDTO);

    }

    protected abstract R generateOnboardingOkNotification(boolean isPartial, EvaluationDTO evaluationDTO);

    abstract String sendNotification(R notificationToSend, EvaluationDTO evaluationDTO, boolean initiativeFetchFailed);


}
