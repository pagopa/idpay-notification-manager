package it.gov.pagopa.notification.manager.service.io.placeholder;

import it.gov.pagopa.notification.manager.dto.EvaluationDTO;
import it.gov.pagopa.notification.manager.dto.OnboardingRejectionReason;
import it.gov.pagopa.notification.manager.model.NotificationContent;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static it.gov.pagopa.notification.manager.dto.OnboardingRejectionReason.OnboardingRejectionReasonCode.*;
import static it.gov.pagopa.notification.manager.dto.OnboardingRejectionReason.OnboardingRejectionReasonType.*;
import static org.junit.jupiter.api.Assertions.*;

class OnboardingPlaceholderTest {

    private final NotificationContentRenderer renderer = new NotificationContentRenderer();

    @Test
    void keys_shouldMatchExpectedNames() {
        assertEquals("initiativeId", OnboardingPlaceholder.INITIATIVE_ID.key());
        assertEquals("initiativeName", OnboardingPlaceholder.INITIATIVE_NAME.key());
        assertEquals("rewardAmount", OnboardingPlaceholder.REWARD_AMOUNT.key());
        assertEquals("managedEntity", OnboardingPlaceholder.MANAGED_ENTITY.key());
        assertEquals("pdndRejectReasonsList", OnboardingPlaceholder.PDND_REJECT_REASONS_LIST.key());
        assertEquals("noRetryRejectReasonsList", OnboardingPlaceholder.NO_RETRY_REJECT_REASONS_LIST.key());
    }

    @Test
    void resolve_initiativeIdAndName() {
        OnboardingPlaceholderContext context = new OnboardingPlaceholderContext(
                "INITIATIVE_123",
                "Nome Iniziativa",
                10000L,
                Collections.emptyList(),
                "https://assistenza.example"
        );

        assertEquals("INITIATIVE_123", OnboardingPlaceholder.INITIATIVE_ID.resolve(context));
        assertEquals("Nome Iniziativa", OnboardingPlaceholder.INITIATIVE_NAME.resolve(context));
    }

    @Test
    void resolve_rewardAmount_whenNotNull() {
        OnboardingPlaceholderContext contextWithBudget = new OnboardingPlaceholderContext(
                "INITIATIVE_123",
                "Nome Iniziativa",
                10000L,
                Collections.emptyList(),
                "https://assistenza.example"
        );
        assertEquals("100", OnboardingPlaceholder.REWARD_AMOUNT.resolve(contextWithBudget));
    }

    @Test
    void resolve_rewardAmount_whenNull() {
        OnboardingPlaceholderContext contextWithoutBudget = new OnboardingPlaceholderContext(
                "INITIATIVE_123",
                "Nome Iniziativa",
                null,
                Collections.emptyList(),
                "https://assistenza.example"
        );
        assertNull(OnboardingPlaceholder.REWARD_AMOUNT.resolve(contextWithoutBudget));
    }

    @Test
    void render_managedEntity_usesAssistanceLinkWhenThereIsNoRejectionReason() {
        assertEquals(
                "[Assistenza](https://assistance.example)",
                renderManagedEntity(null, "https://assistance.example")
        );
    }

    @Test
    void render_managedEntity_usesAssistanceLinkWhenReasonsListIsEmpty() {
        EvaluationDTO evaluation = new EvaluationDTO();
        evaluation.setOnboardingRejectionReasons(Collections.emptyList());
        NotificationContent rendered = renderer.render(
                new NotificationContent("Subject", "%managedEntity%"),
                OnboardingPlaceholderContext.from(evaluation, "https://assistance.example"),
                List.of(OnboardingPlaceholder.values())
        );
        assertEquals("[Assistenza](https://assistance.example)", rendered.markdown());
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

    @Test
    void resolve_pdndRejectReasonsList_includesOnlyPdndTypesAndUsesCustomDetailOrCodeDetail() {
        OnboardingRejectionReason reasonWithCustomDetail = OnboardingRejectionReason.builder()
                .type(ISEE_TYPE_KO)
                .code(ISEE_TYPE_FAIL)
                .authorityLabel("INPS")
                .detail("ISEE difforme")
                .build();

        OnboardingRejectionReason reasonWithCodeDetail = OnboardingRejectionReason.builder()
                .type(BIRTHDATE_KO)
                .code(BIRTHDATE_FAIL)
                .authorityLabel("ANPR")
                .detail(null)
                .build();

        OnboardingRejectionReason ignoredReason = OnboardingRejectionReason.builder()
                .type(AUTOMATED_CRITERIA_FAIL)
                .code(AUTOMATED_CRITERIA_ISEE_FAIL)
                .authorityLabel("ALTRO")
                .detail("Non deve comparire")
                .build();

        OnboardingPlaceholderContext context = new OnboardingPlaceholderContext(
                "ID",
                "Name",
                5000L,
                List.of(reasonWithCustomDetail, reasonWithCodeDetail, ignoredReason),
                "https://assistenza.example"
        );

        String result = OnboardingPlaceholder.PDND_REJECT_REASONS_LIST.resolve(context);
        assertEquals("* INPS : ISEE difforme\n\n* ANPR : Data di nascita non disponibile\n\n", result);
    }

    @Test
    void resolve_pdndRejectReasonsList_allTypesChecked() {
        List<OnboardingRejectionReason> reasons = List.of(
                OnboardingRejectionReason.builder().type(ISEE_TYPE_KO).code(ISEE_TYPE_FAIL).authorityLabel("L1").detail("D1").build(),
                OnboardingRejectionReason.builder().type(BIRTHDATE_KO).code(BIRTHDATE_FAIL).authorityLabel("L2").detail("D2").build(),
                OnboardingRejectionReason.builder().type(RESIDENCE_KO).code(RESIDENCE_FAIL).authorityLabel("L3").detail("D3").build(),
                OnboardingRejectionReason.builder().type(FAMILY_KO).code(FAMILY_FAIL).authorityLabel("L4").detail("D4").build()
        );
        OnboardingPlaceholderContext context = new OnboardingPlaceholderContext("ID", "Name", 100L, reasons, "link");

        String result = OnboardingPlaceholder.PDND_REJECT_REASONS_LIST.resolve(context);
        assertTrue(result.contains("* L1 : D1\n\n"));
        assertTrue(result.contains("* L2 : D2\n\n"));
        assertTrue(result.contains("* L3 : D3\n\n"));
        assertTrue(result.contains("* L4 : D4\n\n"));
    }

    @Test
    void resolve_pdndRejectReasonsList_emptyWhenNoMatchingReasons() {
        OnboardingRejectionReason nonPdndReason = OnboardingRejectionReason.builder()
                .type(AUTOMATED_CRITERIA_FAIL)
                .code(AUTOMATED_CRITERIA_ISEE_FAIL)
                .authorityLabel("ALTRO")
                .detail("Altro")
                .build();

        OnboardingPlaceholderContext context = new OnboardingPlaceholderContext(
                "ID",
                "Name",
                null,
                List.of(nonPdndReason),
                "link"
        );

        assertEquals("", OnboardingPlaceholder.PDND_REJECT_REASONS_LIST.resolve(context));
    }

    @Test
    void resolve_noRetryRejectReasonsList_includesOnlyNoRetryTypesAndUsesCustomDetailOrCodeDetail() {
        OnboardingRejectionReason reasonWithCustomDetail = OnboardingRejectionReason.builder()
                .type(AUTOMATED_CRITERIA_FAIL)
                .code(AUTOMATED_CRITERIA_ISEE_FAIL)
                .detail("Criteri automatici non soddisfatti")
                .build();

        OnboardingRejectionReason reasonWithCodeDetail = OnboardingRejectionReason.builder()
                .type(CONSENSUS_MISSED)
                .code(CONSENSUS_CHECK_TC_FAIL)
                .detail(null)
                .build();

        OnboardingRejectionReason ignoredReason = OnboardingRejectionReason.builder()
                .type(ISEE_TYPE_KO)
                .code(ISEE_TYPE_FAIL)
                .detail("Non deve comparire in no-retry")
                .build();

        OnboardingPlaceholderContext context = new OnboardingPlaceholderContext(
                "ID",
                "Name",
                null,
                List.of(reasonWithCustomDetail, reasonWithCodeDetail, ignoredReason),
                "https://assistenza.example"
        );

        String result = OnboardingPlaceholder.NO_RETRY_REJECT_REASONS_LIST.resolve(context);
        assertEquals("* Criteri automatici non soddisfatti\n\n* Mancato consenso dei termini e condizioni\n\n", result);
    }

    @Test
    void resolve_noRetryRejectReasonsList_allTypesChecked() {
        List<OnboardingRejectionReason> reasons = List.of(
                OnboardingRejectionReason.builder().type(AUTOMATED_CRITERIA_FAIL).code(AUTOMATED_CRITERIA_BIRTHDATE_FAIL).detail("D1").build(),
                OnboardingRejectionReason.builder().type(CONSENSUS_MISSED).code(CONSENSUS_CHECK_PDND_FAIL).detail("D2").build(),
                OnboardingRejectionReason.builder().type(INVALID_REQUEST).code(INVALID_INITIATIVE_ID).detail("D3").build(),
                OnboardingRejectionReason.builder().type(BUDGET_EXHAUSTED).code(INITIATIVE_BUDGET_EXHAUSTED).detail("D4").build()
        );
        OnboardingPlaceholderContext context = new OnboardingPlaceholderContext("ID", "Name", 100L, reasons, "link");

        String result = OnboardingPlaceholder.NO_RETRY_REJECT_REASONS_LIST.resolve(context);
        assertTrue(result.contains("* D1\n\n"));
        assertTrue(result.contains("* D2\n\n"));
        assertTrue(result.contains("* D3\n\n"));
        assertTrue(result.contains("* D4\n\n"));
    }

    @Test
    void resolve_noRetryRejectReasonsList_emptyWhenNoMatchingReasons() {
        OnboardingRejectionReason pdndReason = OnboardingRejectionReason.builder()
                .type(ISEE_TYPE_KO)
                .code(ISEE_TYPE_FAIL)
                .detail("PDND")
                .build();

        OnboardingPlaceholderContext context = new OnboardingPlaceholderContext(
                "ID",
                "Name",
                null,
                List.of(pdndReason),
                "link"
        );

        assertEquals("", OnboardingPlaceholder.NO_RETRY_REJECT_REASONS_LIST.resolve(context));
    }

    @Test
    void renderAllPlaceholders_endToEnd() {
        OnboardingRejectionReason reasonPdnd = OnboardingRejectionReason.builder()
                .type(ISEE_TYPE_KO)
                .code(ISEE_TYPE_FAIL)
                .authorityLabel("INPS")
                .detail("ISEE errato")
                .authority("Comune")
                .build();

        OnboardingRejectionReason reasonNoRetry = OnboardingRejectionReason.builder()
                .type(AUTOMATED_CRITERIA_FAIL)
                .code(AUTOMATED_CRITERIA_ISEE_FAIL)
                .detail("Requisiti non idonei")
                .build();

        EvaluationDTO evaluation = new EvaluationDTO();
        evaluation.setInitiativeId("INIT_42");
        evaluation.setInitiativeName("Bonus Estate");
        evaluation.setBeneficiaryBudgetCents(15000L);
        evaluation.setOnboardingRejectionReasons(List.of(reasonPdnd, reasonNoRetry));

        String template = """
                Iniziativa: %initiativeId% - %initiativeName%
                Importo: %rewardAmount% EUR
                Ente: %managedEntity%
                PDND: %pdndRejectReasonsList%NoRetry: %noRetryRejectReasonsList%""";

        NotificationContent rendered = renderer.render(
                new NotificationContent("Subject", template),
                OnboardingPlaceholderContext.from(evaluation, "https://assistenza.example"),
                List.of(OnboardingPlaceholder.values())
        );

        assertTrue(rendered.markdown().contains("Iniziativa: INIT_42 - Bonus Estate"));
        assertTrue(rendered.markdown().contains("Importo: 150 EUR"));
        assertTrue(rendered.markdown().contains("Ente: Comune"));
        assertTrue(rendered.markdown().contains("PDND: * INPS : ISEE errato\n\n"));
        assertTrue(rendered.markdown().contains("NoRetry: * Requisiti non idonei\n\n"));
    }

    private String renderManagedEntity(OnboardingRejectionReason reason, String assistedLink) {
        return renderManagedEntity(reason, assistedLink, "%managedEntity%");
    }

    private String renderManagedEntity(OnboardingRejectionReason reason, String assistedLink, String markdown) {
        EvaluationDTO evaluation = new EvaluationDTO();
        evaluation.setOnboardingRejectionReasons(reason == null ? null : List.of(reason));
        NotificationContent rendered = renderer.render(
                new NotificationContent("Subject", markdown),
                OnboardingPlaceholderContext.from(evaluation, assistedLink),
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
