package it.gov.pagopa.notification.manager.service;

import it.gov.pagopa.notification.manager.dto.EvaluationDTO;
import it.gov.pagopa.notification.manager.event.OutcomeProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NotificationManagerServiceImpl implements
    NotificationManagerService {

  @Autowired
  OutcomeProducer outcomeProducer;

  @Override
  public void notify(EvaluationDTO evaluationDTO) {
    // It will be implemented in next task
  }

  @Override
  public void addOutcome(EvaluationDTO evaluationDTO) {
    outcomeProducer.sendOutcome(evaluationDTO);
  }
}
