package it.gov.pagopa.notification.manager.dto.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import it.gov.pagopa.notification.manager.dto.EvaluationDTO;
import it.gov.pagopa.notification.manager.dto.NotificationQueueDTO;
import it.gov.pagopa.notification.manager.model.Notification;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = NotificationMapper.class)
class NotificationMapperTest {

  private static final String USER_ID = "USER_ID";
  private static final String INITIATIVE_ID = "INITIATIVE_ID";
  private static final String STATUS = "ONBOARDING_OK";
  private static final LocalDateTime TEST_DATE = LocalDateTime.now();

  private static final EvaluationDTO EVALUATION_DTO =
      new EvaluationDTO(
          USER_ID,
          INITIATIVE_ID,
          INITIATIVE_ID,
          TEST_DATE,
          INITIATIVE_ID,
          STATUS,
          TEST_DATE,
          List.of(),
          new BigDecimal(500),
          INITIATIVE_ID);

  private static final Notification NOTIFICATION =
      Notification.builder()
          .initiativeId(INITIATIVE_ID)
          .userId(USER_ID)
          .onboardingOutcome(STATUS)
          .operationType("ONBOARDING")
          .rejectReasons(List.of())
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
  void queueToNotification() {
    Notification actual = notificationMapper.queueToNotification(NOTIFICATION_QUEUE_DTO);

    NOTIFICATION_QUEUE.setNotificationDate(actual.getNotificationDate());

    assertEquals(NOTIFICATION_QUEUE, actual);
  }
}
