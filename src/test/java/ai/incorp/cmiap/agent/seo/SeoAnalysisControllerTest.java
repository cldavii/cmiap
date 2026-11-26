package ai.incorp.cmiap.agent.seo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SeoAnalysisControllerTest {

    private final SeoAnalysisService service = mock(SeoAnalysisService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        var validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new SeoAnalysisController(service))
                .setValidator(validator)
                .build();
    }

    @Test
    void postAnalyzeReturns200AndRejectsBlankUrl() throws Exception {
        var result = new SeoAnalysisResult(
                "https://example.com", "Example", "Desc", "https://example.com",
                1, 3, 0, 5, 2, 92, "Resumo", "Title ok", "Desc ok", "Content ok", "Tech ok", "Kw ok",
                List.of("Manter estrutura")
        );
        when(service.analyze(any())).thenReturn(result);

        mockMvc.perform(post("/agents/seo/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SeoAnalysisRequest("https://example.com", "automação", null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(92))
                .andExpect(jsonPath("$.url").value("https://example.com"));

        mockMvc.perform(post("/agents/seo/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}

