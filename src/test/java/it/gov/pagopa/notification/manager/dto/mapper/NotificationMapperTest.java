package it.gov.pagopa.notification.manager.dto.mapper;

import it.gov.pagopa.notification.manager.dto.EvaluationDTO;
import it.gov.pagopa.notification.manager.dto.event.NotificationQueueDTO;
import it.gov.pagopa.notification.manager.dto.event.NotificationRefundQueueDTO;
import it.gov.pagopa.notification.manager.model.Notification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static it.gov.pagopa.notification.manager.constants.NotificationConstants.AnyNotificationConsumer.SubTypes.REFUND;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = NotificationMapper.class)
class NotificationMapperTest {

  private static final String USER_ID = "USER_ID";
  private static final String INITIATIVE_ID = "INITIATIVE_ID";
  private static final String ORGANIZATION_NAME = "ORGANIZATION_NAME";
  private static final String STATUS = "ONBOARDING_OK";
  private static final LocalDate TEST_DATE = LocalDate.now();

  private static final EvaluationDTO EVALUATION_DTO =
      new EvaluationDTO(
          USER_ID,
          INITIATIVE_ID,
          INITIATIVE_ID,
          TEST_DATE,
          INITIATIVE_ID,
          ORGANIZATION_NAME,
          STATUS,
          TEST_DATE.atStartOfDay(),
          TEST_DATE.atStartOfDay(),
          List.of(),
          50000L, 1L);

  private static final Notification NOTIFICATION =
      Notification.builder()
          .initiativeId(INITIATIVE_ID)
          .initiativeName(INITIATIVE_ID)
          .userId(USER_ID)
          .onboardingOutcome(STATUS)
          .operationType("ONBOARDING")
          .rejectReasons(List.of())
          .organizationName(ORGANIZATION_NAME)
          .build();

  private static final Notification NOTIFICATION_QUEUE =
      Notification.builder()
          .initiativeId(INITIATIVE_ID)
          .userId(USER_ID)
          .build();
  private static final NotificationQueueDTO NOTIFICATION_QUEUE_DTO =
      NotificationQueueDTO.builder()
          .initiativeId(INITIATIVE_ID)
          .userId(USER_ID)
          .build();

  @Autowired
  NotificationMapper notificationMapper;

  @Test
  void evaluationToNotification() {
    Notification actual = notificationMapper.evaluationToNotification(EVALUATION_DTO);

    NOTIFICATION.setNotificationDate(actual.getNotificationDate());

    assertEquals(NOTIFICATION, actual);
  }

  @Test
  void toEntity(){
    Notification actual = notificationMapper.toEntity(NOTIFICATION_QUEUE_DTO);
    NOTIFICATION_QUEUE.setNotificationDate(actual.getNotificationDate());

    assertEquals(NOTIFICATION_QUEUE, actual);
  }

  @Test
  void notificationRefund_toEntity(){
    NotificationRefundQueueDTO notificationRefundQueueDTO = new NotificationRefundQueueDTO();
    notificationRefundQueueDTO.setInitiativeId(INITIATIVE_ID);
    notificationRefundQueueDTO.setUserId(USER_ID);
    notificationRefundQueueDTO.setOperationType("REFUND");
    notificationRefundQueueDTO.setRefundReward(10L);
    notificationRefundQueueDTO.setRefundDate(LocalDate.now());
    notificationRefundQueueDTO.setStatus("ACCEPTED");

    Notification actual = notificationMapper.toEntity(notificationRefundQueueDTO);
    Notification expected = Notification.builder()
            .initiativeId(INITIATIVE_ID)
            .userId(USER_ID)
            .operationType(REFUND)
            .notificationDate(actual.getNotificationDate())
            .refundReward(BigDecimal.TEN)
            .refundStatus("ACCEPTED").build();

    assertEquals(expected, actual);
  }

}
