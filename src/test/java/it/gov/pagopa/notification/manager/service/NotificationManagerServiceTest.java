package it.gov.pagopa.notification.manager.service;

import it.gov.pagopa.notification.manager.dto.EvaluationDTO;
import it.gov.pagopa.notification.manager.event.OutcomeProducer;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(value = {NotificationManagerService.class})
class NotificationManagerServiceTest {

  private static final EvaluationDTO EVALUTATION_DTO =
      new EvaluationDTO("", "", "", LocalDateTime.now(), null);
  @Autowired NotificationManagerService notificationManagerService;

  @MockBean OutcomeProducer outcomeProducer;

  @Test
  void sendToQueue() {
    Mockito.doNothing().when(outcomeProducer).sendOutcome(EVALUTATION_DTO);

    notificationManagerService.addOutcome(EVALUTATION_DTO);

    Mockito.verify(outcomeProducer, Mockito.times(1)).sendOutcome(EVALUTATION_DTO);
  }
}
