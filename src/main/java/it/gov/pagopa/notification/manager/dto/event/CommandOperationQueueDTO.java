package it.gov.pagopa.notification.manager.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@AllArgsConstructor
@Builder
public class CommandOperationQueueDTO {
    private String entityId;
    private String operationType;
    private LocalDateTime operationTime;
    private Map<String, String> additionalParams;
}
