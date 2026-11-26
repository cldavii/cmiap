package ai.incorp.cmiap.agent.reports;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReportKpiControllerTest {

    private final ReportKpiService service = mock(ReportKpiService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        var validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new ReportKpiController(service))
                .setValidator(validator)
                .build();
    }

    @Test
    void postKpisReturns200WithReportAndRejectsInvalidBody() throws Exception {
        var goal = new ReportRequestDTO.Goal(new BigDecimal("100"), ReportRequestDTO.GoalDirection.AT_LEAST);
        var obs = new ReportRequestDTO.Observation(ReportRequestDTO.Source.META, "meta-q4", new BigDecimal("120"));
        var metric = new ReportRequestDTO.Metric(
                ReportRequestDTO.Channel.PAID, "Leads", "leads", goal, List.of(obs), List.of("ctx-1"));
        var request = new ReportRequestDTO(
                "CMIAP", LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 26), List.of(metric), false);

        var response = new ReportResponseDTO(
                "CMIAP",
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 26),
                List.of(new ReportResponseDTO.MetricResult(
                        ReportRequestDTO.Channel.PAID,
                        "Leads",
                        "leads",
                        new BigDecimal("120"),
                        goal,
                        ReportResponseDTO.Status.GOAL_MET,
                        List.of(obs),
                        List.of("ctx-1")
                )),
                null
        );
        when(service.report(any())).thenReturn(response);

        mockMvc.perform(post("/agents/reports/kpis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product").value("CMIAP"))
                .andExpect(jsonPath("$.metrics[0].status").value("GOAL_MET"));

        mockMvc.perform(post("/agents/reports/kpis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}

