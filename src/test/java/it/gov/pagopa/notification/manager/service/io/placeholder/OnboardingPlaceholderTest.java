package it.gov.pagopa.notification.manager.service.io.placeholder;

import it.gov.pagopa.notification.manager.dto.EvaluationDTO;
import it.gov.pagopa.notification.manager.dto.OnboardingRejectionReason;
import it.gov.pagopa.notification.manager.model.NotificationContent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static it.gov.pagopa.notification.manager.dto.OnboardingRejectionReason.OnboardingRejectionReasonCode.FAMILY_CRITERIA_FAIL;
import static it.gov.pagopa.notification.manager.dto.OnboardingRejectionReason.OnboardingRejectionReasonCode.REJECTION_REASON_INITIATIVE_ENDED;
import static it.gov.pagopa.notification.manager.dto.OnboardingRejectionReason.OnboardingRejectionReasonType.INVALID_REQUEST;
import static org.junit.jupiter.api.Assertions.assertEquals;

class OnboardingPlaceholderTest {

    private final NotificationContentRenderer renderer = new NotificationContentRenderer();

    @Test
    void render_managedEntity_usesAssistanceLinkWhenThereIsNoRejectionReason() {
        assertEquals(
                "[Assistenza](https://assistance.example)",
                renderManagedEntity(null, "https://assistance.example")
        );
    }

    @Test
    void render_managedEntity_usesAssistanceLinkForInitiativeEndedReason() {
        OnboardingRejectionReason reason = OnboardingRejectionReason.builder()
                .type(INVALID_REQUEST)
                .code(REJECTION_REASON_INITIATIVE_ENDED)
                .authority("Comune di Test")
                .build();

        assertEquals(
                "Comune di Test",
                renderManagedEntity(reason, "https://assistance.example")
        );
    }

    @Test
    void render_managedEntity_usesAuthorityWhenPresent() {
        OnboardingRejectionReason reason = reason("");

        assertEquals("N.A.", renderManagedEntity(reason, "https://assistance.example"));
    }

    @Test
    void render_managedEntity_preservesWhitespaceAuthority() {
        OnboardingRejectionReason reason = reason("  ");

        assertEquals("before[  ]after", renderManagedEntity(
                reason,
                "https://assistance.example",
                "before[%managedEntity%]after"
        ));
    }

    @Test
    void render_managedEntity_usesAssistanceLinkWhenAuthorityIsNull() {
        OnboardingRejectionReason reason = reason(null);

        assertEquals(
                "[Assistenza](https://assistance.example)",
                renderManagedEntity(reason, "https://assistance.example")
        );
    }

    private String renderManagedEntity(OnboardingRejectionReason reason, String assistedLink) {
        return renderManagedEntity(reason, assistedLink, "%managedEntity%");
    }

    private String renderManagedEntity(OnboardingRejectionReason reason, String assistedLink, String markdown) {
        EvaluationDTO evaluation = new EvaluationDTO();
        evaluation.setOnboardingRejectionReasons(reason == null ? null : List.of(reason));
        NotificationContent rendered = renderer.render(
                new NotificationContent("Subject", markdown),
                new OnboardingPlaceholderContext(evaluation, assistedLink),
                List.of(OnboardingPlaceholder.values())
        );
        return rendered.markdown();
    }

    private OnboardingRejectionReason reason(String authority) {
        return OnboardingRejectionReason.builder()
                .type(INVALID_REQUEST)
                .code(FAMILY_CRITERIA_FAIL)
                .authority(authority)
                .build();
    }
}
