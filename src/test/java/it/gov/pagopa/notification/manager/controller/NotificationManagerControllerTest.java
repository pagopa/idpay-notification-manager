package it.gov.pagopa.notification.manager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.notification.manager.dto.EvaluationDTO;
import it.gov.pagopa.notification.manager.service.NotificationManagerService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(
    value = {NotificationManagerController.class},
    excludeAutoConfiguration = SecurityAutoConfiguration.class)
class NotificationManagerControllerTest {

  private static final String BASE_URL = "http://localhost:8080/idpay/notifications/";

  private static final EvaluationDTO EVALUATION_DTO =
      new EvaluationDTO("", "", "", LocalDateTime.now(), null);

  @MockBean NotificationManagerService notificationManagerServiceMock;

  @Autowired protected MockMvc mvc;

  @Autowired ObjectMapper objectMapper;

  @Test
  void addOutcome_ok() throws Exception {

    Mockito.doNothing().when(notificationManagerServiceMock).addOutcome(EVALUATION_DTO);

    mvc.perform(
            MockMvcRequestBuilders.put(BASE_URL)
                .content(objectMapper.writeValueAsString(EVALUATION_DTO))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andReturn();
  }
}
