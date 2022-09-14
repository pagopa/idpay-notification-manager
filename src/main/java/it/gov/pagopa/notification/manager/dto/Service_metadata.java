package it.gov.pagopa.notification.manager.dto;

import lombok.Data;

@Data
public class Service_metadata {
  private String description;
  private String web_url;
  private String app_ios;
  private String app_android;
  private String tos_url;
  private String privacy_url;
  private String address;
  private String phone;
  private String email;
  private String pec;
  private String cta;
  private String token_name;
  private String support_url;
  private String scope;
  private String category;
  private String custom_special_flow;

}
