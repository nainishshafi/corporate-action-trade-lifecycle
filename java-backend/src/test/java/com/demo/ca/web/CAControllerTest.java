package com.demo.ca.web;

import com.demo.ca.application.CALifecycleUseCase;
import com.demo.ca.application.CorporateActionNotFoundException;
import com.demo.ca.application.CorporateActionView;
import com.demo.ca.domain.CorporateAction;
import com.demo.ca.domain.CorporateActionStateException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CAController.class)
class CAControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CALifecycleUseCase lifecycleUseCase;

    private static CorporateActionView analyzedView() {
        return new CorporateActionView(
            1L,
            "AAPL",
            "STOCK_SPLIT",
            "2:1",
            LocalDate.of(2026, 11, 23),
            LocalDate.of(2026, 11, 24),
            LocalDate.of(2026, 11, 28),
            CorporateAction.Status.ANALYZED,
            "AI detected a 2:1 split.",
            List.of(new CorporateActionView.PositionImpactView(10L, "ACC-001", new BigDecimal("100.0"), new BigDecimal("200.0"))));
    }

    @Test
    void pendingEndpointReturnsAnalysesForHitlReview() throws Exception {
        when(lifecycleUseCase.getPendingActions()).thenReturn(List.of(analyzedView()));

        mockMvc.perform(get("/api/ca/pending"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].securityId").value("AAPL"))
            .andExpect(jsonPath("$[0].status").value("ANALYZED"))
            .andExpect(jsonPath("$[0].aiAnalysis").value("AI detected a 2:1 split."))
            .andExpect(jsonPath("$[0].impacts[0].accountId").value("ACC-001"))
            .andExpect(jsonPath("$[0].impacts[0].newQuantity").value(200.0));
    }

    @Test
    void approveEndpointDelegatesToUseCase() throws Exception {
        mockMvc.perform(post("/api/ca/42/approve"))
            .andExpect(status().isOk());

        verify(lifecycleUseCase).approveAction(42L);
    }

    @Test
    void approveReturns404WhenActionDoesNotExist() throws Exception {
        doThrow(new CorporateActionNotFoundException(42L)).when(lifecycleUseCase).approveAction(42L);

        mockMvc.perform(post("/api/ca/42/approve"))
            .andExpect(status().isNotFound());
    }

    @Test
    void approveReturns409WhenActionIsNotAwaitingApproval() throws Exception {
        doThrow(new CorporateActionStateException(42L, CorporateAction.Status.APPROVED,
            "Only analyzed corporate actions can be approved"))
            .when(lifecycleUseCase).approveAction(42L);

        mockMvc.perform(post("/api/ca/42/approve"))
            .andExpect(status().isConflict());
    }
}
