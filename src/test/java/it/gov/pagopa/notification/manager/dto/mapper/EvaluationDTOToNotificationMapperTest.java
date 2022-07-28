package it.gov.pagopa.notification.manager.dto.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import it.gov.pagopa.notification.manager.dto.EvaluationDTO;
import it.gov.pagopa.notification.manager.dto.MessageContent;
import it.gov.pagopa.notification.manager.dto.NotificationDTO;
import it.gov.pagopa.notification.manager.model.Notification;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = EvaluationDTOToNotificationMapper.class)
class EvaluationDTOToNotificationMapperTest {

  private static final String USER_ID = "USER_ID";
  private static final String INITIATIVE_ID = "INITIATIVE_ID";
  private static final String STATUS = "ONBOARDING_OK";

  private static final EvaluationDTO EVALUTATION_DTO =
      new EvaluationDTO(USER_ID, INITIATIVE_ID, STATUS, LocalDateTime.now(), null);

  private static final Notification NOTIFICATION =
      Notification.builder()
          .initiativeId(INITIATIVE_ID)
          .userId(USER_ID)
          .onboardingOutcome(STATUS)
          .rejectReasons(null)
          .build();

  @Autowired EvaluationDTOToNotificationMapper evaluationDTOToNotificationMapper;

  @Test
  void map() {
    Notification actual = evaluationDTOToNotificationMapper.map(EVALUTATION_DTO);

    NOTIFICATION.setNotificationDate(actual.getNotificationDate());

    assertEquals(NOTIFICATION, actual);
  }
}
