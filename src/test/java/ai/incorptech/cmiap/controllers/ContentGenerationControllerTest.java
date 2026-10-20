package ai.incorptech.cmiap.controllers;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ai.incorptech.cmiap.application.modules.contentGenerator.ContentGeneratorService;
import ai.incorptech.cmiap.presentation.controller.ContentGenerationController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ContentGenerationController.class)
@AutoConfigureMockMvc(addFilters = false)
class ContentGenerationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ContentGeneratorService contentGeneratorService;

    @Test
    void geraConteudoComOsDadosInformados() throws Exception {
        given(contentGeneratorService.chat(anyString()))
                .willReturn("Sua instituição pode organizar os documentos em um fluxo integrado.");

        mockMvc.perform(post("/ai/content/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topic": "Gestão documental integrada",
                                  "format": "Post para LinkedIn",
                                  "product": "Incorp Next",
                                  "channel": "LinkedIn",
                                  "objective": "Educar gestores",
                                  "audience": "Conselhos profissionais",
                                  "referenceFacts": "Fluxos documentais configuráveis"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content")
                        .value("Sua instituição pode organizar os documentos em um fluxo integrado."));

        verify(contentGeneratorService).chat(org.mockito.ArgumentMatchers.contains("Gestão documental integrada"));
    }

    @Test
    void rejeitaTemaEmBrancoSemChamarOAgente() throws Exception {
        mockMvc.perform(post("/ai/content/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topic": "   ",
                                  "format": "Post para LinkedIn"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(contentGeneratorService, never()).chat(anyString());
    }
}