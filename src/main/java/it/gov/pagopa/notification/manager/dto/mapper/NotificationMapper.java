package it.gov.pagopa.notification.manager.dto.mapper;

import it.gov.pagopa.notification.manager.constants.NotificationConstants;
import it.gov.pagopa.notification.manager.dto.EmailMessageDTO;
import it.gov.pagopa.notification.manager.dto.EvaluationDTO;
import it.gov.pagopa.notification.manager.dto.event.AnyOfNotificationQueueDTO;
import it.gov.pagopa.notification.manager.dto.event.NotificationRefundQueueDTO;
import it.gov.pagopa.notification.manager.dto.event.NotificationReminderQueueDTO;
import it.gov.pagopa.notification.manager.model.Notification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
public class NotificationMapper {
    public Notification evaluationToNotification(EvaluationDTO evaluationDTO) {
      return Notification.builder()
              .notificationDate(LocalDateTime.now())
              .initiativeId(evaluationDTO.getInitiativeId())
              .initiativeName(evaluationDTO.getInitiativeName())
              .userId(evaluationDTO.getUserId())
              .onboardingOutcome(evaluationDTO.getStatus())
              .operationType(NotificationConstants.AnyNotificationConsumer.SubTypes.ONBOARDING)
              .rejectReasons(evaluationDTO.getOnboardingRejectionReasons())
              .organizationName(evaluationDTO.getOrganizationName())
              .channel(evaluationDTO.getChannel())
              .build();
    }

    public EmailMessageDTO notificationToEmailMessageDTO(Notification notification) {
        return EmailMessageDTO.builder()
                .templateName(notification.getTemplateName())
                .templateValues(notification.getTemplateValues())
                .subject(notification.getSubject())
                .content(notification.getContent())
                .senderEmail(notification.getSenderEmail())
                .recipientEmail(notification.getRecipientEmail())
                .build();
    }


    public Notification toEntity(AnyOfNotificationQueueDTO anyOfNotificationQueueDTO){
      Notification notification = Notification.builder().build();
      BeanUtils.copyProperties(anyOfNotificationQueueDTO, notification);
      notification.setNotificationDate(LocalDateTime.now());
      if(anyOfNotificationQueueDTO instanceof NotificationRefundQueueDTO notificationRefundOnQueueDTO){
        notification.setRefundStatus(notificationRefundOnQueueDTO.getStatus());
        notification.setRefundReward(BigDecimal.valueOf(notificationRefundOnQueueDTO.getRefundReward()));
      }
      return notification;
    }

    //public Notification createNotificationFromNotificationReminderQuequeDTO(EmailMessageDTO emailMessageDTO,
    //                                                                         NotificationReminderQueueDTO notificationReminderQueueDTO){
//
    //    Notification notification = toEntity(notificationReminderQueueDTO);
    //    notification.setTemplateName(emailMessageDTO.getTemplateName());
    //    notification.setTemplateValues(emailMessageDTO.getTemplateValues());
    //    notification.setSubject(emailMessageDTO.getSubject());
    //    notification.setSenderEmail(emailMessageDTO.getSenderEmail());
    //    notification.setRecipientEmail(emailMessageDTO.getRecipientEmail());
    //    return notification;
    //}

    public Notification createNotificationFromEmailMessageDTO(EmailMessageDTO emailMessageDTO, EvaluationDTO evaluationDTO){

        Notification notification = evaluationToNotification(evaluationDTO);
        notification.setTemplateName(emailMessageDTO.getTemplateName());
        notification.setTemplateValues(emailMessageDTO.getTemplateValues());
        notification.setSubject(emailMessageDTO.getSubject());
        notification.setSenderEmail(emailMessageDTO.getSenderEmail());
        notification.setRecipientEmail(emailMessageDTO.getRecipientEmail());
        return notification;
    }
}
