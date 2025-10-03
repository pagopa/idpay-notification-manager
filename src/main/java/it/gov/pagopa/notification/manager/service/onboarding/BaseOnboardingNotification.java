package it.gov.pagopa.notification.manager.service.onboarding;

import it.gov.pagopa.notification.manager.dto.EvaluationDTO;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import static it.gov.pagopa.notification.manager.constants.NotificationConstants.*;

@Slf4j
public abstract class BaseOnboardingNotification<R> {

    public String processNotification(EvaluationDTO evaluationDTO){
        R notificationToSend = switch (evaluationDTO.getStatus()){
            case STATUS_ONBOARDING_OK -> processOnboardingOk(evaluationDTO);
            case STATUS_ONBOARDING_JOINED -> processOnboardingJoined(evaluationDTO);
            case STATUS_ONBOARDING_KO -> processOnboardingKo(evaluationDTO);
            default -> {log.info("[NOTIFY] Unsupported notification for status {} for user {}", evaluationDTO.getStatus(), evaluationDTO.getUserId());
                            yield null;
            }
        };

        if(notificationToSend != null){
            return sendNotification(notificationToSend, evaluationDTO);
        }

        return null;
    }

    abstract R processOnboardingJoined(EvaluationDTO evaluationDTO);

    abstract R processOnboardingKo(EvaluationDTO evaluationDTO);

    abstract String flowName();

    private R processOnboardingOk(EvaluationDTO evaluationDTO) {
        boolean isBudgetAboveThreshold = evaluationDTO.getBeneficiaryBudgetCents() != null && evaluationDTO.getBeneficiaryBudgetCents() == 10000;
        boolean isPartial = Boolean.TRUE.equals(evaluationDTO.getVerifyIsee()) && isBudgetAboveThreshold;
        return generateOnboardingOkNotification(isPartial, evaluationDTO);

    }

    protected abstract R generateOnboardingOkNotification(boolean isPartial, EvaluationDTO evaluationDTO);

    abstract R createNotification(EvaluationDTO evaluationDTO, String subject, String body, Map<String, String> bodyValues);

    abstract String sendNotification (R notificationToSend, EvaluationDTO evaluationDTO);
}
