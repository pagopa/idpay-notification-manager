package it.gov.pagopa.notification.manager.dto.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationAuthPaymentDTO extends NotificationQueueDTO{
    private String trxId;
    private OffsetDateTime trxDate;
    private String merchantId;
    private String merchantFiscalCode;
    private String status;
    private Long reward;
    private Long amountCents;
    private List<String> rejectionReasons;
}
