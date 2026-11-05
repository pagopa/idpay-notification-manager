package it.gov.pagopa.notification.manager.service.io.placeholder;

import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.notification.manager.dto.OnboardingRejectionReason;

import java.util.function.Function;

public enum OnboardingPlaceholder implements NotificationPlaceholder<OnboardingPlaceholderContext> {

    INITIATIVE_ID("initiativeId", context -> context.evaluation().getInitiativeId()),
    INITIATIVE_NAME("initiativeName", context -> context.evaluation().getInitiativeName()),
    REWARD_AMOUNT("rewardAmount", OnboardingPlaceholder::rewardAmount),
    MANAGED_ENTITY("managedEntity", OnboardingPlaceholder::managedEntity);

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
        Long cents = context.evaluation().getBeneficiaryBudgetCents();
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
        var reasons = context.evaluation().getOnboardingRejectionReasons();
        return reasons == null || reasons.isEmpty() ? null : reasons.getFirst();
    }
}
