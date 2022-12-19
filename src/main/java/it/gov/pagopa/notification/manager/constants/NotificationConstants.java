package it.gov.pagopa.notification.manager.constants;

public class NotificationConstants {

  public static final String STATUS_ONBOARDING_OK = "ONBOARDING_OK";
  public static final String INITIATIVE_NAME_KEY = "initiativeName";
  public static final String MARKDOWN_TAG = "%";
  public static final String MARKDOWN_NA = "N.A.";
  public static final String STATUS_ONBOARDING_KO = "ONBOARDING_KO";
  public static final String REQUEST_PDV = "[NOTIFY] Sending request to pdv";
  public static final String FEIGN_KO = "[NOTIFY] [%d] Cannot send request: %s";
  public static final String IO_TOKENS = "[NOTIFY] Getting IO Tokens";

  private NotificationConstants(){}

  public static final class AnyNotificationConsumer{
    private AnyNotificationConsumer(){}
    public static final class SubTypes{
      private SubTypes(){}
      public static final String ALLOWED_CITIZEN_PUBLISH = "ALLOWED_CITIZEN_PUBLISH";
      public static final String CHECKIBAN_KO = "CHECKIBAN_KO";
      public static final String REFUND = "REFUND";
    }
  }

}
