package it.gov.pagopa.notification.manager.service.io.placeholder;

import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.notification.manager.dto.OnboardingRejectionReason;

import java.util.function.Function;

import static it.gov.pagopa.notification.manager.dto.OnboardingRejectionReason.OnboardingRejectionReasonCode.REJECTION_REASON_INITIATIVE_ENDED;

public enum OnboardingPlaceholder implements NotificationPlaceholder<OnboardingPlaceholderContext> {

    INITIATIVE_ID("initiativeId", context -> context.evaluation().getInitiativeId()),
    INITIATIVE_NAME("initiativeName", context -> context.evaluation().getInitiativeName()),
    REWARD_AMOUNT("rewardAmount", context -> {
        Long cents = context.evaluation().getBeneficiaryBudgetCents();
        return cents == null ? null : CommonUtilities.centsToEuro(cents).toString();
    }),
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

    private static String managedEntity(OnboardingPlaceholderContext context) {
        OnboardingRejectionReason firstReason = firstReason(context);
        if (firstReason == null || REJECTION_REASON_INITIATIVE_ENDED.equals(firstReason.getCode())) {
            return "%managedEntity%"; // TODO: works like old code, maybe should be change to return null so the renderer marks "N.A."
        }
        if (firstReason.getAuthority() != null) {
            return firstReason.getAuthority();
        }
        return "[Assistenza](" + context.assistedLink() + ")";
    }

    private static OnboardingRejectionReason firstReason(OnboardingPlaceholderContext context) {
        var reasons = context.evaluation().getOnboardingRejectionReasons();
        return reasons == null || reasons.isEmpty() ? null : reasons.getFirst();
    }
}
