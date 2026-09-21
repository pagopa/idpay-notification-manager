package it.gov.pagopa.notification.manager.service.onboarding;

import feign.FeignException;
import it.gov.pagopa.notification.manager.connector.initiative.InitiativeRestConnector;
import it.gov.pagopa.notification.manager.dto.EvaluationDTO;
import it.gov.pagopa.notification.manager.dto.VerifyDTO;
import it.gov.pagopa.notification.manager.dto.initiative.InitiativeNotificationDTO;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import static it.gov.pagopa.notification.manager.constants.NotificationConstants.*;


@Slf4j
public abstract class BaseOnboardingNotification<R> {

    private final InitiativeRestConnector initiativeRestConnector;

    protected BaseOnboardingNotification(InitiativeRestConnector initiativeRestConnector) {
        this.initiativeRestConnector = initiativeRestConnector;
    }

    public String processNotification(EvaluationDTO evaluationDTO){
        String sanitizedUserId = sanitizeString(evaluationDTO.getUserId());
        String sanitizedStatus = sanitizeString(evaluationDTO.getStatus());

        InitiativeNotificationDTO initiativeNotificationDTO = null;
        boolean initiativeFetchFailed = false;
        try {
             initiativeNotificationDTO = initiativeRestConnector.getInitiativeDetailInfo(evaluationDTO.getInitiativeId());
        } catch (FeignException e) {
                log.error("[PROCESS_ONBOARDING_NOTIFICATION] Failed to retrieve initiativeDetail from initiative service.");
                initiativeFetchFailed = true;
        }

        evaluationDTO.setEmailFlux(initiativeNotificationDTO.getEmailFlux());

        R notificationToSend = switch (evaluationDTO.getStatus()){
            case STATUS_ONBOARDING_OK -> processOnboardingOk(evaluationDTO);
            case STATUS_ONBOARDING_JOINED -> processOnboardingJoined(evaluationDTO);
            case STATUS_ONBOARDING_KO -> processOnboardingKo(evaluationDTO);
            default -> {log.info("[NOTIFY] Unsupported notification for status {} for user {}", sanitizedStatus, sanitizedUserId);
                            yield null;
            }
        };

        if(notificationToSend != null){
            return sendNotification(notificationToSend, evaluationDTO, initiativeFetchFailed);
        }

        return null;
    }

    abstract R processOnboardingJoined(EvaluationDTO evaluationDTO);

    abstract R processOnboardingKo(EvaluationDTO evaluationDTO);

    private R processOnboardingOk(EvaluationDTO evaluationDTO) {
        boolean isPartial = Boolean.FALSE;
        if(evaluationDTO.getVerifies() != null && !evaluationDTO.getVerifies().isEmpty()){
            for(VerifyDTO verify : evaluationDTO.getVerifies()){
                if(evaluationDTO.getBeneficiaryBudgetCents() != null && verify.getBeneficiaryBudgetCentsMin() == evaluationDTO.getBeneficiaryBudgetCents()){
                    isPartial = Boolean.TRUE;
                    break;
                }
        }

        }

        return generateOnboardingOkNotification(isPartial, evaluationDTO);

    }

    protected abstract R generateOnboardingOkNotification(boolean isPartial, EvaluationDTO evaluationDTO);

    abstract R createNotification(EvaluationDTO evaluationDTO, String subject, String body, Map<String, String> bodyValues);

    abstract String sendNotification(R notificationToSend, EvaluationDTO evaluationDTO, boolean initiativeFetchFailed);

    public static String sanitizeString(String str){
        return str == null? null: str.replaceAll("[\\r\\n]", "").replaceAll("[^\\w\\s-]", "");
    }

}
