package it.gov.pagopa.notification.manager.constants;

public class NotificationConstants {

  public static final String STATUS_ONBOARDING_OK = "ONBOARDING_OK";
  public static final String INITIATIVE_NAME_KEY = "initiativeName";
  public static final String MARKDOWN_TAG = "%";
  public static final String MARKDOWN_NA = "N.A.";
  public static final String STATUS_ONBOARDING_KO = "ONBOARDING_KO";

  private NotificationConstants(){}

  public static final class AnyNotificationConsumer{
    public static final class SubTypes{
      public static final String ALLOWED_CITIZEN_PUBLISH = "ALLOWED_CITIZEN_PUBLISH";
      public static final String CHECKIBAN_KO = "CHECKIBAN_KO";
    }
  }

}
