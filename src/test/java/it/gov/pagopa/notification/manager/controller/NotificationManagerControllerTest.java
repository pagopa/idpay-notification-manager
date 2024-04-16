package it.gov.pagopa.notification.manager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.notification.manager.constants.NotificationConstants;
import it.gov.pagopa.notification.manager.dto.EvaluationDTO;
import it.gov.pagopa.notification.manager.service.NotificationManagerService;
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

import java.time.LocalDate;
import java.util.List;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(
    value = {NotificationManagerController.class},
    excludeAutoConfiguration = SecurityAutoConfiguration.class)
class NotificationManagerControllerTest {

  private static final String BASE_URL = "http://localhost:8080/idpay/notifications/";

  private static final String USER_ID = "USER_ID";
  private static final String INITIATIVE_ID = "INITIATIVE_ID";

  private static final LocalDate TEST_DATE = LocalDate.now();
  private static final EvaluationDTO EVALUATION_DTO =
      new EvaluationDTO(
          USER_ID,
          INITIATIVE_ID,
          INITIATIVE_ID,
          TEST_DATE,
          INITIATIVE_ID,
          NotificationConstants.STATUS_ONBOARDING_OK,
          TEST_DATE.atStartOfDay(),
          TEST_DATE.atStartOfDay(),
          List.of(),
              50000L, 1L);

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

  @Test
  void forceScheduling_ok() throws Exception {
    Mockito.doNothing().when(notificationManagerServiceMock).recoverKoNotifications();

    mvc.perform(MockMvcRequestBuilders.get(BASE_URL+"/recover/start"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();
  }
}
