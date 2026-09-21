package ai.incorptech.cmiap.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.Test;

class ChatServiceTest {

	private final ChatModel chatModel = mock(ChatModel.class);

	private final ChatService chatService = new ChatService(chatModel);

	@Test
	void delegaOPromptAoModeloEDevolveAResposta() {
		when(chatModel.chat("Resuma o relatorio de vendas do trimestre"))
				.thenReturn("As vendas cresceram 12% no trimestre.");

		String resposta = chatService.reply("Resuma o relatorio de vendas do trimestre");

		assertThat(resposta).isEqualTo("As vendas cresceram 12% no trimestre.");
	}

	@Test
	void rejeitaPromptEmBrancoSemChamarOModelo() {
		assertThatThrownBy(() -> chatService.reply("   "))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("prompt");

		verify(chatModel, never()).chat(anyString());
	}

	@Test
	void rejeitaPromptNuloSemChamarOModelo() {
		assertThatThrownBy(() -> chatService.reply(null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("prompt");

		verify(chatModel, never()).chat(anyString());
	}
}
