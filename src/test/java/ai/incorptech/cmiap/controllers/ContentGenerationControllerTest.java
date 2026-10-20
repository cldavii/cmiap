package ai.incorptech.cmiap.controllers;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ai.incorptech.cmiap.application.modules.contentGenerator.ContentGeneratorService;
import ai.incorptech.cmiap.infrastructure.ai.memory.AgentChatMemoryProvider;
import ai.incorptech.cmiap.infrastructure.security.provider.JwtService;
import ai.incorptech.cmiap.presentation.controller.ContentGenerationController;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ContentGenerationController.class)
class ContentGenerationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ContentGeneratorService contentGeneratorService;

    @MockitoBean
    private AgentChatMemoryProvider contentGeneratorChatMemoryProvider;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void geraConteudoComOsDadosInformados() throws Exception {
        UUID conversationId = UUID.fromString("251b24d9-2686-4e68-aa72-193f3d66fa62");
        given(contentGeneratorChatMemoryProvider.memoryIdFor("writer@example.com", conversationId))
                .willReturn("isolated-memory-id");
        given(contentGeneratorService.chat(anyString(), anyString()))
                .willReturn("Sua instituição pode organizar os documentos em um fluxo integrado.");

        mockMvc.perform(post("/ai/content/generate")
                        .with(user("writer@example.com"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "conversationId": "%s",
                                  "topic": "Gestão documental integrada",
                                  "format": "Post para LinkedIn",
                                  "product": "Incorp Next",
                                  "channel": "LinkedIn",
                                  "objective": "Educar gestores",
                                  "audience": "Conselhos profissionais",
                                  "referenceFacts": "Fluxos documentais configuráveis"
                                }
                                """.formatted(conversationId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationId").value(conversationId.toString()))
                .andExpect(jsonPath("$.content")
                        .value("Sua instituição pode organizar os documentos em um fluxo integrado."));

        verify(contentGeneratorService).chat(
                eq("isolated-memory-id"),
                org.mockito.ArgumentMatchers.contains("Gestão documental integrada"));
    }

    @Test
    void criaEDevolveUmIdDeConversaQuandoNaoInformado() throws Exception {
        given(contentGeneratorChatMemoryProvider.memoryIdFor(eq("writer@example.com"),
                org.mockito.ArgumentMatchers.any(UUID.class)))
                .willReturn("isolated-memory-id");
        given(contentGeneratorService.chat(anyString(), anyString())).willReturn("Conteúdo gerado");

        mockMvc.perform(post("/ai/content/generate")
                        .with(user("writer@example.com"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topic": "Gestão documental",
                                  "format": "Post"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationId").isNotEmpty())
                .andExpect(jsonPath("$.content").value("Conteúdo gerado"));

        verify(contentGeneratorChatMemoryProvider).memoryIdFor(
                eq("writer@example.com"), org.mockito.ArgumentMatchers.any(UUID.class));
    }

    @Test
    void rejeitaTemaEmBrancoSemChamarOAgente() throws Exception {
        mockMvc.perform(post("/ai/content/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user("writer@example.com"))
                        .with(csrf())
                        .content("""
                                {
                                  "topic": "   ",
                                  "format": "Post para LinkedIn"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(contentGeneratorService, never()).chat(anyString(), anyString());
    }

    @Test
    void apagaSomenteAMemoriaDoUsuarioAutenticadoEConversaInformada() throws Exception {
        UUID conversationId = UUID.fromString("251b24d9-2686-4e68-aa72-193f3d66fa62");

        mockMvc.perform(delete("/ai/content/conversations/{conversationId}", conversationId)
                        .with(user("writer@example.com"))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(contentGeneratorChatMemoryProvider)
                .clearConversation("writer@example.com", conversationId);
    }
}