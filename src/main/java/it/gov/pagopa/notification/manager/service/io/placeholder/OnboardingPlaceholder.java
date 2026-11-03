package it.gov.pagopa.notification.manager.service.io.placeholder;

import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.notification.manager.dto.OnboardingRejectionReason;
import org.springframework.util.StringUtils;

import java.util.function.Function;

public enum OnboardingPlaceholder implements NotificationPlaceholder<OnboardingPlaceholderContext> {

    INITIATIVE_ID("initiativeId", context -> context.evaluation().getInitiativeId()),
    INITIATIVE_NAME("initiativeName", context -> context.evaluation().getInitiativeName()),
    REWARD_AMOUNT("rewardAmount", context -> {
        Long cents = context.evaluation().getBeneficiaryBudgetCents();
        return cents == null ? null : CommonUtilities.centsToEuro(cents).toString();
    }),
    MANAGED_ENTITY("managedEntity", OnboardingPlaceholder::managedEntity),
    REASON("reason", OnboardingPlaceholder::reason);

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
        if (firstReason != null && StringUtils.hasText(firstReason.getAuthority())) {
            return firstReason.getAuthority();
        }
        return StringUtils.hasText(context.assistedLink())
                ? "[Assistenza](" + context.assistedLink() + ")"
                : null;
    }

    private static String reason(OnboardingPlaceholderContext context) {
        OnboardingRejectionReason firstReason = firstReason(context);
        if (firstReason == null) {
            return null;
        }
        if (StringUtils.hasText(firstReason.getDetail())) {
            return firstReason.getDetail();
        }
        return firstReason.getCode() == null ? null : firstReason.getCode().getDetail();
    }

    private static OnboardingRejectionReason firstReason(OnboardingPlaceholderContext context) {
        var reasons = context.evaluation().getOnboardingRejectionReasons();
        return reasons == null || reasons.isEmpty() ? null : reasons.getFirst();
    }
}
