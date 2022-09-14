package it.gov.pagopa.notification.manager.dto;

import java.util.ArrayList;
import lombok.Data;

@Data
public class ServiceResource {

    private String service_name;
    private String department_name;
    private String organization_name;
    private String organization_fiscal_code;
    ArrayList<Object> authorized_cidrs = new ArrayList<>();
    private float version;
    private boolean require_secure_channels;
    private boolean is_visible;
    Service_metadata Service_metadataObject;
    private String id;
    private String service_id;
    ArrayList<Object> authorized_recipients = new ArrayList<>();
    private float max_allowed_payment_amount;
    private String primary_key;
    private String secondary_key;

}
