package it.gov.pagopa.notification.manager.event;

import it.gov.pagopa.notification.manager.dto.EvaluationDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

@Component
public class OutcomeProducer {

  @Value("${spring.cloud.stream.bindings.notificationQueue-out-0.binder}")
  private String binder;

  private final StreamBridge streamBridge;

  public OutcomeProducer(StreamBridge streamBridge){
    this.streamBridge = streamBridge;
  }

  public void sendOutcome(EvaluationDTO evaluationDTO){
    streamBridge.send("notificationQueue-out-0", binder, evaluationDTO);
  }

}
