package it.gov.pagopa.notification.manager.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import lombok.Data;

@Data
public class ServiceResource {

    @JsonProperty("service_name")
    private String serviceName;
    @JsonProperty("department_name")
    private String departmentName;
    @JsonProperty("organization_name")
    private String organizationName;
    @JsonProperty("organization_fiscal_code")
    private String organizationFiscalCode;
    @JsonProperty("authorized_cidrs")
    ArrayList<Object> authorizedCidrs = new ArrayList<>();
    private float version;
    @JsonProperty("require_secure_channels")
    private boolean requireSecureChannels;
    @JsonProperty("is_visible")
    private boolean isVisible;
    @JsonProperty("Service_metadataObject")
    ServiceMetadata serviceMetadataObject;
    private String id;
    @JsonProperty("service_id")
    private String serviceId;
    @JsonProperty("authorized_recipients")
    ArrayList<Object> authorizedRecipients = new ArrayList<>();
    @JsonProperty("max_allowed_payment_amount")
    private float maxAllowedPaymentAmount;
    @JsonProperty("primary_key")
    private String primaryKey;
    @JsonProperty("secondary_key")
    private String secondaryKey;

}
