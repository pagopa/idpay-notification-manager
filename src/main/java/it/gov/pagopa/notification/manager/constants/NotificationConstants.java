package it.gov.pagopa.notification.manager.constants;

public class NotificationConstants {

  public static final String STATUS_ONBOARDING_OK = "ONBOARDING_OK";
  public static final String STATUS_ONBOARDING_JOINED = "JOINED";
  public static final String STATUS_ONBOARDING_DEMANDED = "DEMANDED";
  public static final String INITIATIVE_NAME_KEY = "initiativeName";
  public static final String INITIATIVE_ID_KEY = "initiativeId";
  public static final String REWARD_AMOUNT_KEY = "rewardAmount";
  public static final String REASON_KEY = "reason";
  public static final String MANAGED_ENTITY_KEY = "managedEntity";
  public static final String MARKDOWN_TAG = "%";
  public static final String MARKDOWN_NA = "N.A.";
  public static final String STATUS_ONBOARDING_KO = "ONBOARDING_KO";
  public static final String REQUEST_PDV = "[NOTIFY] Sending request to pdv";
  public static final String FEIGN_KO = "[NOTIFY] [%d] Cannot send request: %s";
  public static final String IO_TOKENS = "[NOTIFY] Getting IO Tokens";
  public static final String NOTIFICATION_STATUS_OK = "OK";
  public static final String NOTIFICATION_STATUS_KO = "KO";
  public static final String NOTIFICATION_STATUS_RECOVER = "RECOVER";
  public static final String OPERATION_TYPE_DELETE_INITIATIVE = "DELETE_INITIATIVE";

  public static final String OPERATION_TYPE_REMINDER = "REMINDER";

  private NotificationConstants(){}

  public static final class ExceptionCode {
    public static final String GENERIC_ERROR = "NOTIFICATION_MANAGER_GENERIC_ERROR";
    public static final String TOO_MANY_REQUESTS = "NOTIFICATION_MANAGER_TOO_MANY_REQUESTS";
    public static final String INVALID_REQUEST = "NOTIFICATION_MANAGER_INVALID_REQUEST";

    private ExceptionCode() {}
  }

  public static final class AnyNotificationConsumer{
    private AnyNotificationConsumer(){}
    public static final class SubTypes{
      private SubTypes(){}
      public static final String ALLOWED_CITIZEN_PUBLISH = "ALLOWED_CITIZEN_PUBLISH";
      public static final String CHECKIBAN_KO = "CHECKIBAN_KO";
      public static final String REFUND = "REFUND";
      public static final String SUSPENSION = "SUSPENSION";
      public static final String READMISSION = "READMISSION";
      public static final String ONBOARDING = "ONBOARDING";
      public static final String REMINDER = "REMINDER";
    }
  }
  public static final class EmailTemplates {
    private EmailTemplates() {}

    public static final String EMAIL_OUTCOME_OK = "Email_BEL_EsitoOk";
    public static final String EMAIL_OUTCOME_PARTIAL = "Email_BEL_EsitoParziale";
    public static final String EMAIL_OUTCOME_THANKS = "Email_BEL_Thanks";
    public static final String EMAIL_OUTCOME_THREE_DAY_REMINDER = "Email_BEL_ThreeDayReminder";
    public static final String EMAIL_OUTCOME_FAMILY_UNIT = "Email_BEL_FamilyUnit";
    public static final String EMAIL_OUTCOME_GENERIC_ERROR = "Email_BEL_GenericError";
  }
}
