package it.gov.pagopa.notification.manager.service.io.placeholder;

import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.notification.manager.dto.OnboardingRejectionReason;

import java.util.List;
import java.util.function.Function;

public enum OnboardingPlaceholder implements NotificationPlaceholder<OnboardingPlaceholderContext> {

    INITIATIVE_ID("initiativeId", OnboardingPlaceholderContext::initiativeId),
    INITIATIVE_NAME("initiativeName", OnboardingPlaceholderContext::initiativeName),
    REWARD_AMOUNT("rewardAmount", OnboardingPlaceholder::rewardAmount),
    MANAGED_ENTITY("managedEntity", OnboardingPlaceholder::managedEntity),
    PDND_REJECT_REASONS_LIST("pdndRejectReasonsList", OnboardingPlaceholder::pdndRejectReasonsList),
    NO_RETRY_REJECT_REASONS_LIST("noRetryRejectReasonsList", OnboardingPlaceholder::noRetryRejectReasonsList);

    private final String key;
    private final Function<OnboardingPlaceholderContext, String> resolver;

    OnboardingPlaceholder(String key, Function<OnboardingPlaceholderContext, String> resolver) {
        this.key = key;
        this.resolver = resolver;
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public String resolve(OnboardingPlaceholderContext source) {
        return resolver.apply(source);
    }

    private static String rewardAmount(OnboardingPlaceholderContext context) {
        Long cents = context.beneficiaryBudgetCents();
        return cents == null ? null : String.valueOf(CommonUtilities.centsToEuro(cents));
    }

    private static String managedEntity(OnboardingPlaceholderContext context) {
        OnboardingRejectionReason firstReason = firstReason(context);
        if (firstReason != null && firstReason.getAuthority() != null) {
            return firstReason.getAuthority();
        }
        return "[Assistenza](" + context.assistedLink() + ")";
    }

    private static OnboardingRejectionReason firstReason(OnboardingPlaceholderContext context) {
        var reasons = context.onboardingRejectionReasons();
        return reasons == null || reasons.isEmpty() ? null : reasons.getFirst();
    }

    /*
     * PDND_REJECT_REASONS_LIST START
     */
    private static final List<OnboardingRejectionReason.OnboardingRejectionReasonType> PDND_REJECT_REASONS_TYPE_LIST =
            List.of(OnboardingRejectionReason.OnboardingRejectionReasonType.ISEE_TYPE_KO,
                    OnboardingRejectionReason.OnboardingRejectionReasonType.BIRTHDATE_KO,
                    OnboardingRejectionReason.OnboardingRejectionReasonType.RESIDENCE_KO,
                    OnboardingRejectionReason.OnboardingRejectionReasonType.FAMILY_KO);

    private static String pdndRejectReasonsList(OnboardingPlaceholderContext onboardingPlaceholderContext) {
        final StringBuilder builder = new StringBuilder();
        onboardingPlaceholderContext.onboardingRejectionReasons().stream()
                .filter(
                        reason ->
                                PDND_REJECT_REASONS_TYPE_LIST.contains(reason.getType()))
                .toList()
                .forEach(
                        reason ->
                                builder
                                        .append("* ")
                                        .append(reason.getAuthorityLabel())
                                        .append(" : ")
                                        .append(reason.getDetail() != null
                                                ? reason.getDetail()
                                                : reason.getCode().getDetail())
                                        .append("\n\n"));
        return builder.toString();
    }
    /*
     * PDND_REJECT_REASONS_LIST END
     */

    /*
     * NO_RETRY_REJECT_REASONS_LIST START
     */
    private static final List<OnboardingRejectionReason.OnboardingRejectionReasonType> NO_RETRY_REJECT_REASONS_TYPE_LIST =
            List.of(OnboardingRejectionReason.OnboardingRejectionReasonType.AUTOMATED_CRITERIA_FAIL,
                    OnboardingRejectionReason.OnboardingRejectionReasonType.CONSENSUS_MISSED,
                    OnboardingRejectionReason.OnboardingRejectionReasonType.INVALID_REQUEST,
                    OnboardingRejectionReason.OnboardingRejectionReasonType.BUDGET_EXHAUSTED);

    private static String noRetryRejectReasonsList(OnboardingPlaceholderContext onboardingPlaceholderContext) {
        final StringBuilder builder = new StringBuilder();
        onboardingPlaceholderContext.onboardingRejectionReasons().stream()
                .filter(
                        reason ->
                                NO_RETRY_REJECT_REASONS_TYPE_LIST.contains(reason.getType()))
                .toList()
                .forEach(
                        reason ->
                                builder
                                        .append("* ")
                                        .append(reason.getDetail() != null
                                                ? reason.getDetail()
                                                : reason.getCode().getDetail())
                                        .append("\n\n"));
        return builder.toString();
    }
    /*
     * NO_RETRY_REJECT_REASONS_LIST END
     */
}
