package ai.incorptech.cmiap.controllers;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ai.incorptech.cmiap.services.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ChatController.class)
class ChatControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ChatService chatService;

	@Test
	void devolve200ComARespostaDoAgente() throws Exception {
		given(chatService.reply("Sugira 3 ideias de campanha para o lancamento"))
				.willReturn("1. Teaser em video. 2. Webinar. 3. Co-marketing.");

		mockMvc.perform(post("/chat")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"message\":\"Sugira 3 ideias de campanha para o lancamento\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.reply").value("1. Teaser em video. 2. Webinar. 3. Co-marketing."));
	}

	@Test
	void devolve400QuandoOPromptEstaEmBranco() throws Exception {
		mockMvc.perform(post("/chat")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"message\":\"   \"}"))
				.andExpect(status().isBadRequest());

		verify(chatService, never()).reply(anyString());
	}

	@Test
	void devolve400QuandoOCampoMessageEstaAusente() throws Exception {
		mockMvc.perform(post("/chat")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest());

		verify(chatService, never()).reply(anyString());
	}
}
