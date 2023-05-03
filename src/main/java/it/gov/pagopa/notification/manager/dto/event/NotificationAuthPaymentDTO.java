package it.gov.pagopa.notification.manager.dto.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationAuthPaymentDTO extends NotificationQueueDTO{
    private String idPayment; //TODO aggiungere campi
}
