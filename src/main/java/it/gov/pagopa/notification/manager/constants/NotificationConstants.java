package it.gov.pagopa.notification.manager.constants;

public class NotificationConstants {

  public static final String STATUS_ONBOARDING_OK = "ONBOARDING_OK";
  public static final String INITIATIVE_NAME_KEY = "initiativeName";
  public static final String INITIATIVE_ID_KEY = "initiativeId";
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

  public static final class CtaConstant {

    private CtaConstant(){}

    public static final String START = "---\n";
    public static final String IT = "it:\n    ";
    public static final String CTA_1_IT = "cta_1: \n        ";
    public static final String TEXT_IT = "text: \"Vai all'iniziativa\"\n        ";
    public static final String ACTION_IT = "action: \"ioit://idpay/initiative/%initiativeId%";
    public static final String EN = "\"\nen:\n    ";
    public static final String CTA_1_EN = "cta_1: \n        ";
    public static final String TEXT_EN = "text: \"Go to the bonus page\"\n        ";
    public static final String ACTION_EN = "action: \"ioit://idpay/initiative/%initiativeId%";
    public static final String END = "\"\n---";

    public static String getCta(){
      StringBuilder sb = new StringBuilder();
      sb.append(START);
      sb.append(IT);
      sb.append(CTA_1_IT);
      sb.append(TEXT_IT);
      sb.append(ACTION_IT);
      sb.append(EN);
      sb.append(CTA_1_EN);
      sb.append(TEXT_EN);
      sb.append(ACTION_EN);
      sb.append(END);
      return sb.toString();
    }

  }

}
